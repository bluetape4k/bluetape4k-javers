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
    File.write(@root.join("docs/manual/en/modules/sample.md"), "# Sample\n")
    File.write(@root.join("docs/manual/ko/modules/sample.md"), "# 샘플\n")
    File.write(@root.join("src/Sample.kt"), "class Sample\n")
    write_pair("sample", "release")
    git("init", "-q")
    git("config", "user.email", "test@example.com")
    git("config", "user.name", "Shared Diagram Test")
    git("config", "tag.gpgSign", "false")
    git("add", ".")
    git("commit", "-qm", "release fixture")
    @release_commit = git("rev-parse", "HEAD")
    git("tag", "0.2.1")
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

    assert_includes contract.errors, "manual releaseRef 0.2.1 resolves to #{@release_commit}, expected #{'0' * 40}"
  end

  def test_check_reports_selected_asset_missing_from_release
    write_pair("future", "snapshot")
    write_inventory([entry("future", manual: "selected")])

    assert_includes contract.errors, "future: missing release source 0.2.1:docs/images/readme-diagrams/future.svg"
    assert_includes contract.errors, "future: missing release source 0.2.1:docs/images/readme-diagrams/future.png"
  end

  def test_check_reports_release_digest_mismatch
    contract.sync!
    File.binwrite(@root.join("docs/manual/assets/readme-diagrams/sample.png"), "different")

    assert_includes contract.errors, "sample: release and mirror PNG digests differ"
  end

  def test_check_reports_orphan_mirror
    contract.sync!
    File.write(@root.join("docs/manual/assets/readme-diagrams/orphan.svg"), "<svg/>")

    assert_includes contract.errors, "orphan mirror asset: orphan.svg"
  end

  def test_sync_copies_release_pair_not_snapshot_pair_and_skips_deferred_pair
    write_pair("deferred", "snapshot-deferred")
    write_inventory([entry("sample", manual: "selected"), entry("deferred", manual: "deferred")])

    contract.sync!

    assert_equal "release-svg", File.read(@root.join("docs/manual/assets/readme-diagrams/sample.svg"))
    assert_equal "release-png", File.read(@root.join("docs/manual/assets/readme-diagrams/sample.png"))
    refute File.exist?(@root.join("docs/manual/assets/readme-diagrams/deferred.svg"))
    refute File.exist?(@root.join("docs/manual/assets/readme-diagrams/deferred.png"))
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
      YAML.dump({ "stableMinor" => "0.2", "releaseRef" => "0.2.1", "releaseCommit" => commit }),
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
