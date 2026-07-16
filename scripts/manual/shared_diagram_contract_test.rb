#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "minitest/autorun"
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
    write_pair("sample", "canonical")
    write_inventory([entry("sample", manual: "selected")])
  end

  def teardown
    FileUtils.remove_entry(@tmp)
  end

  def test_inventory_contains_all_25_canonical_pairs
    entries = 21.times.map { |index| entry("selected-#{index}", manual: "selected") } +
              4.times.map { |index| entry("deferred-#{index}", manual: "deferred") }
    entries.each { |row| write_pair(row.fetch("canonical"), row.fetch("canonical")) }
    write_inventory(entries)

    assert_equal 25, contract.entries.size
    assert_equal 21, contract.entries.count(&:selected?)
    assert_equal 4, contract.entries.count(&:deferred?)
  end

  def test_rejects_unsafe_relative_paths
    write_inventory([entry("sample", manual: "selected").merge("canonical" => "../escape")])

    error = assert_raises(SharedDiagrams::ContractError) { contract.entries }

    assert_match(/unsafe canonical path/, error.message)
  end

  def test_check_reports_missing_canonical_pair
    FileUtils.rm(@root.join("docs/images/readme-diagrams/sample.png"))

    assert_includes contract.errors, "sample: missing canonical PNG"
  end

  def test_check_reports_digest_mismatch
    contract.sync!
    File.binwrite(@root.join("docs/manual/assets/readme-diagrams/sample.png"), "different")

    assert_includes contract.errors, "sample: canonical and mirror PNG digests differ"
  end

  def test_check_reports_orphan_mirror
    contract.sync!
    File.write(@root.join("docs/manual/assets/readme-diagrams/orphan.svg"), "<svg/>")

    assert_includes contract.errors, "orphan mirror asset: orphan.svg"
  end

  def test_sync_copies_selected_pairs_and_skips_deferred_pairs
    write_pair("deferred", "deferred")
    write_inventory([entry("sample", manual: "selected"), entry("deferred", manual: "deferred")])

    contract.sync!

    assert_equal "canonical-svg", File.read(@root.join("docs/manual/assets/readme-diagrams/sample.svg"))
    assert_equal "canonical-png", File.read(@root.join("docs/manual/assets/readme-diagrams/sample.png"))
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
      YAML.dump({ "schemaVersion" => 1, "diagrams" => entries }),
    )
  end

  def write_pair(id, content)
    File.write(@root.join("docs/images/readme-diagrams/#{id}.svg"), "#{content}-svg")
    File.binwrite(@root.join("docs/images/readme-diagrams/#{id}.png"), "#{content}-png")
  end
end
