#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "minitest/autorun"
require "open3"
require "pathname"
require "tmpdir"
require "yaml"
require_relative "shared_diagram_contract"

class SharedDiagramContractTest < Minitest::Test
  def setup
    @tmp = Dir.mktmpdir("shared-diagrams")
    @root = Pathname.new(@tmp)
    FileUtils.mkdir_p(@root.join("docs/images/readme-diagrams"))
    FileUtils.mkdir_p(@root.join("docs/manual/assets/readme-diagrams"))
    FileUtils.mkdir_p(@root.join("docs/manual/en/modules"))
    FileUtils.mkdir_p(@root.join("docs/manual/ko/modules"))
    FileUtils.mkdir_p(@root.join("src"))
    File.write(@root.join("docs/manual/en/modules/sample.md"), "[![Sample](../../assets/readme-diagrams/sample.png)](../../assets/readme-diagrams/sample.svg)\n")
    File.write(@root.join("docs/manual/ko/modules/sample.md"), "[![샘플](../../assets/readme-diagrams/sample.png)](../../assets/readme-diagrams/sample.svg)\n")
    File.write(@root.join("src/Sample.kt"), "class Sample\n")
    write_pair("sample", "release")
    git("init", "-q")
    git("config", "user.email", "test@example.com")
    git("config", "user.name", "Shared Diagram Test")
    git("config", "tag.gpgSign", "false")
    git("add", ".")
    git("commit", "-qm", "release fixture")
    @release_commit = git("rev-parse", "HEAD")
    git("tag", "0.3.0")
    write_manifest(@release_commit)
    write_pair("sample", "snapshot")
    write_inventory([entry("sample", manual: "selected")])
  end

  def teardown
    FileUtils.remove_entry(@tmp)
  end

  def test_inventory_contains_three_release_diagrams_and_22_deferred_diagrams
    entries = 3.times.map { |index| entry("selected-#{index}", manual: "selected") } +
              22.times.map { |index| entry("deferred-#{index}", manual: "deferred") }
    entries.each { |row| write_pair(row.fetch("canonical"), row.fetch("canonical")) }
    write_inventory(entries)

    assert_equal 25, contract.entries.size
    assert_equal 3, contract.entries.count(&:selected?)
    assert_equal 22, contract.entries.count(&:deferred?)
  end

  def test_rejects_unsafe_relative_paths
    write_inventory([entry("sample", manual: "selected").merge("canonical" => "../escape")])

    error = assert_raises(SharedDiagrams::ContractError) { contract.entries }

    assert_match(/unsafe canonical path/, error.message)
  end

  def test_rejects_release_ref_commit_mismatch
    write_manifest("0" * 40)

    assert_includes contract.errors, "manual releaseRef 0.3.0 resolves to #{@release_commit}, expected #{'0' * 40}"
  end

  def test_check_reports_selected_asset_missing_from_release
    write_pair("future", "snapshot")
    write_inventory([entry("future", manual: "selected")])

    assert_includes contract.errors, "future: missing release source 0.3.0:docs/images/readme-diagrams/future.svg"
    assert_includes contract.errors, "future: missing release source 0.3.0:docs/images/readme-diagrams/future.png"
  end

  def test_check_reports_manual_mirror_directory
    contract.sync!
    FileUtils.mkdir_p(@root.join("docs/manual/assets/readme-diagrams"))
    File.binwrite(@root.join("docs/manual/assets/readme-diagrams/sample.png"), "different")

    assert_includes contract.errors, "manual mirror directory still exists: docs/manual/assets/readme-diagrams"
  end

  def test_sync_links_release_pair_and_removes_mirror
    write_pair("deferred", "snapshot-deferred")
    write_inventory([entry("sample", manual: "selected"), entry("deferred", manual: "deferred")])

    contract.sync!

    page = File.read(@root.join("docs/manual/en/modules/sample.md"))
    assert_includes page, "https://raw.githubusercontent.com/bluetape4k/bluetape4k-javers/#{@release_commit}/docs/images/readme-diagrams/sample.png"
    assert_includes page, "https://github.com/bluetape4k/bluetape4k-javers/blob/#{@release_commit}/docs/images/readme-diagrams/sample.svg"
    refute File.exist?(@root.join("docs/manual/assets/readme-diagrams"))
    refute_includes File.read(@root.join("docs/manual/manifest.yaml")), "assets/readme-diagrams/"
    assert_empty contract.errors
  end

  private

  def contract
    SharedDiagrams::Contract.new(root: @root, inventory_path: @root.join("docs/manual/shared-diagrams.yaml"))
  end

  def entry(id, manual:)
    row = {
      "id" => id,
      "kind" => "architecture",
      "canonical" => id,
      "manual" => manual,
      "sourcePaths" => ["src/Sample.kt"],
    }
    if manual == "selected"
      row["manualPages"] = {
        "en" => "docs/manual/en/modules/sample.md",
        "ko" => "docs/manual/ko/modules/sample.md",
      }
    end
    row
  end

  def write_inventory(entries)
    File.write(
      @root.join("docs/manual/shared-diagrams.yaml"),
      YAML.dump({ "schemaVersion" => 3, "sourcePolicy" => "manual-release", "diagrams" => entries }),
    )
  end

  def write_manifest(commit)
    File.write(
      @root.join("docs/manual/manifest.yaml"),
      YAML.dump({
        "repository" => "bluetape4k-javers",
        "stableMinor" => "0.3",
        "releaseRef" => "0.3.0",
        "releaseCommit" => commit,
        "overview" => { "assets" => ["assets/readme-diagrams/sample.png", "assets/readme-diagrams/sample.svg"] },
      }),
    )
  end

  def write_pair(id, content)
    File.write(@root.join("docs/images/readme-diagrams/#{id}.svg"), "#{content}-svg")
    File.binwrite(@root.join("docs/images/readme-diagrams/#{id}.png"), "#{content}-png")
  end

  def git(*arguments)
    stdout, stderr, status = Open3.capture3("git", "-C", @root.to_s, *arguments)
    raise "git #{arguments.join(' ')} failed: #{stderr}" unless status.success?

    stdout.strip
  end
end
