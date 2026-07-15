require "fileutils"
require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "release_contract"
require_relative "validate_release_manuals"

class ReleaseContractTest < Minitest::Test
  SHA = "bffe19439ca891fa5301a76421bdef7ba75252a0"

  def runner(tree:, type: "tag", sha: SHA)
    lambda do |arguments|
      case arguments.first
      when "cat-file" then ["#{type}\n", true]
      when "rev-parse" then ["#{sha}\n", true]
      when "ls-tree" then [tree.join("\n") + "\n", true]
      else ["", false]
      end
    end
  end

  def test_rejects_missing_tag
    contract = ManualDocs::ReleaseContract.new(
      repository_root: Dir.pwd, tag: "0.2.1", expected_sha: SHA, git_runner: ->(_arguments) { ["", false] },
    )
    assert contract.errors.any? { |error| error.include?("tag not found") }
  end

  def test_rejects_wrong_tag_commit
    contract = ManualDocs::ReleaseContract.new(
      repository_root: Dir.pwd, tag: "0.2.1", expected_sha: SHA,
      git_runner: runner(tree: [], sha: "a" * 40),
    )
    assert contract.errors.any? { |error| error.include?("expected #{SHA}") }
  end

  def test_rejects_source_link_outside_release_tree
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"), "[source](../../../javers-core/src/Missing.kt)\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.2.1", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert contract.errors.any? { |error| error.include?("release path not found") }
    end
  end

  def test_rejects_reference_style_source_link_outside_release_tree
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"), <<~MARKDOWN)
        [source][code]

        [code]: ../../../javers-core/src/Missing.kt
      MARKDOWN
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.2.1", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert contract.errors.any? { |error| error.include?("release path not found") }
    end
  end

  def test_rejects_manifest_source_path_outside_release_tree
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual"))
      manifest = File.join(root, "docs/manual/manifest.yaml")
      File.write(manifest, YAML.dump("modules" => [{ "id" => "core", "sourceDir" => "javers-core", "sourcePaths" => ["missing"] }]))
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.2.1", expected_sha: SHA, manifest_path: manifest,
        git_runner: runner(tree: ["javers-core/build.gradle.kts"]),
      )
      assert contract.errors.any? { |error| error.include?("sourcePath not found in release tree") }
    end
  end

  def test_rejects_evidence_path_outside_release_tree
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual"))
      manifest = File.join(root, "docs/manual/manifest.yaml")
      File.write(manifest, YAML.dump("modules" => [], "evidence" => [{ "id" => "benchmark", "path" => "docs/benchmark/missing.json" }]))
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.2.1", expected_sha: SHA, manifest_path: manifest,
        git_runner: runner(tree: []),
      )
      result = contract.validate
      assert result.errors.any? { |error| error.include?("evidence path not found in release tree") }
      assert_equal 1, result.evidence_path_count
    end
  end

  def test_final_validation_rejects_missing_inventory_file
    Dir.mktmpdir do |root|
      result = ManualDocs::ReleaseContract::ValidationResult.new(errors: [], checked_count: 0, source_path_count: 0, evidence_path_count: 0)
      validator = ManualDocs::ReleaseManualValidator.new(
        repository_root: root, tag: "0.2.1", expected_sha: SHA,
        inventory_path: File.join(root, "missing.json"), manifest_path: File.join(root, "docs/manual/manifest.yaml"),
        release_contract: Struct.new(:validate).new(result),
      )
      assert validator.errors.any? { |error| error.include?("release inventory not found") }
    end
  end
end
