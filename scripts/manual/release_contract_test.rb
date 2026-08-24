require "fileutils"
require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "release_contract"
require_relative "validate_release_manuals"

class ReleaseContractTest < Minitest::Test
  SHA = "978d0490fc438570e7520643aed50e20614772d1"

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
      repository_root: Dir.pwd, tag: "0.3.0", expected_sha: SHA, git_runner: ->(_arguments) { ["", false] },
    )
    assert contract.errors.any? { |error| error.include?("tag not found") }
  end

  def test_rejects_wrong_tag_commit
    contract = ManualDocs::ReleaseContract.new(
      repository_root: Dir.pwd, tag: "0.3.0", expected_sha: SHA,
      git_runner: runner(tree: [], sha: "a" * 40),
    )
    assert contract.errors.any? { |error| error.include?("expected #{SHA}") }
  end

  def test_rejects_source_link_outside_release_tree
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"), "[source](../../../javers-core/src/Missing.kt)\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
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
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert contract.errors.any? { |error| error.include?("release path not found") }
    end
  end

  def test_rejects_github_blob_autolink_with_unpinned_ref
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<https://github.com/bluetape4k/bluetape4k-javers/blob/develop/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert contract.errors.any? { |error| error.include?("source link commit develop") }
    end
  end

  def test_accepts_pinned_github_blob_autolink_in_release_tree
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<https://github.com/bluetape4k/bluetape4k-javers/blob/#{SHA}/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert_empty contract.errors
      assert_equal 1, contract.validate.checked_count
    end
  end

  def test_rejects_mixed_case_github_blob_autolink_with_unpinned_ref
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<HTTPS://GitHub.com/bluetape4k/bluetape4k-javers/blob/develop/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert contract.errors.any? { |error| error.include?("source link commit develop") }
      assert_equal 1, contract.validate.checked_count
    end
  end

  def test_accepts_mixed_case_scheme_and_host_for_pinned_blob_autolink
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<hTtPs://gItHuB.cOm/bluetape4k/bluetape4k-javers/blob/#{SHA}/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert_empty contract.errors
      assert_equal 1, contract.validate.checked_count
    end
  end

  def test_rejects_mixed_case_github_owner_and_repository_with_unpinned_ref
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<https://github.com/BlueTape4K/BlueTape4K-JaVers/blob/develop/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert contract.errors.any? { |error| error.include?("source link commit develop") }
      assert_equal 1, contract.validate.checked_count
    end
  end

  def test_accepts_mixed_case_github_identity_when_ref_is_pinned
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<https://github.com/BlueTape4K/BlueTape4K-JaVers/blob/#{SHA}/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert_empty contract.errors
      assert_equal 1, contract.validate.checked_count
    end
  end

  def test_rejects_raw_github_autolink_with_unpinned_ref
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<https://raw.githubusercontent.com/BlueTape4K/BlueTape4K-JaVers/develop/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert contract.errors.any? { |error| error.include?("source link commit develop") }
      assert_equal 1, contract.validate.checked_count
    end
  end

  def test_accepts_raw_github_autolink_with_pinned_ref_and_release_path
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/en"))
      File.write(File.join(root, "docs/manual/en/index.md"),
        "<HTTPS://RAW.GITHUBUSERCONTENT.COM/BlueTape4K/BlueTape4K-JaVers/#{SHA}/javers-core/src/Present.kt>\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: ["javers-core/src/Present.kt"]),
      )
      assert_empty contract.errors
      assert_equal 1, contract.validate.checked_count
    end
  end

  def test_excludes_current_manual_from_the_immutable_release_contract
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual/current/en"))
      File.write(File.join(root, "docs/manual/current/en/index.md"), "[module](modules/javers-core.md)\n")
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        git_runner: runner(tree: []),
      )

      assert_empty contract.errors
    end
  end

  def test_final_validation_requires_complete_content
    with_release_fixture(content_status: "planned") do |validator|
      assert validator.errors.any? { |error| error.include?("contentStatus must be complete") }
    end
    with_release_fixture(content_status: "complete") do |validator|
      assert_empty validator.errors
    end
  end

  def test_cli_parses_allow_planned
    assert_respond_to ManualDocs::ReleaseManualValidator, :parse_cli
    return unless ManualDocs::ReleaseManualValidator.respond_to?(:parse_cli)

    parsed = ManualDocs::ReleaseManualValidator.parse_cli(
      ["0.3.0", SHA, "build/manual/release-module-inventory.json", "--allow-planned"],
    )
    assert_equal true, parsed.fetch(:allow_planned)
    assert_equal "0.3.0", parsed.fetch(:tag)
    assert_equal SHA, parsed.fetch(:expected_sha)
    assert_equal "build/manual/release-module-inventory.json", parsed.fetch(:inventory_path)
  end

  def test_authoring_release_tree_mode_accepts_planned_content
    with_release_fixture(content_status: "planned", allow_planned: true) do |validator|
      assert_empty validator.errors
    end
  end

  def test_authoring_release_tree_mode_preserves_source_and_evidence_errors
    release_errors = [
      "core: sourcePath not found in release tree: javers-core/src/Missing.kt",
      "benchmark: evidence path not found in release tree: docs/benchmark/missing.json",
    ]
    with_release_fixture(content_status: "planned", allow_planned: true, release_errors: release_errors) do |validator|
      assert_equal release_errors.sort, validator.errors
    end
  end

  def test_rejects_manifest_source_path_outside_release_tree
    Dir.mktmpdir do |root|
      FileUtils.mkdir_p(File.join(root, "docs/manual"))
      manifest = File.join(root, "docs/manual/manifest.yaml")
      File.write(manifest, YAML.dump("modules" => [{ "id" => "core", "sourceDir" => "javers-core", "sourcePaths" => ["missing"] }]))
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA, manifest_path: manifest,
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
        repository_root: root, tag: "0.3.0", expected_sha: SHA, manifest_path: manifest,
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
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        inventory_path: File.join(root, "missing.json"), manifest_path: File.join(root, "docs/manual/manifest.yaml"),
        release_contract: Struct.new(:validate).new(result),
      )
      assert validator.errors.any? { |error| error.include?("release inventory not found") }
    end
  end

  private

  def with_release_fixture(content_status:, allow_planned: false, release_errors: [])
    Dir.mktmpdir do |root|
      row = {
        "id" => "javers-core", "gradlePath" => ":javers-core", "projectName" => "javers-core",
        "sourceDir" => "javers-core", "kind" => "library", "group" => "foundation",
        "artifact" => "io.github.bluetape4k.javers:javers-core", "status" => "stable",
        "sourcePaths" => ["javers-core"], "en" => "en/modules/javers-core.md", "ko" => "ko/modules/javers-core.md",
      }
      FileUtils.mkdir_p(File.join(root, "javers-core"))
      File.write(File.join(root, "javers-core/build.gradle.kts"), "")
      documents = { "en" => ["en/index.md"], "ko" => ["ko/index.md"] }
      (documents.values.flatten + [row["en"], row["ko"]]).each do |relative|
        path = File.join(root, "docs/manual", relative)
        FileUtils.mkdir_p(File.dirname(path))
        File.write(path, "manual\n")
      end
      evidence = File.join(root, "docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json")
      FileUtils.mkdir_p(File.dirname(evidence))
      File.write(evidence, "{}\n")
      manifest = {
        "schemaVersion" => 2, "repository" => "bluetape4k-javers", "releaseRef" => "0.3.0",
        "stableVersion" => "0.3.0", "stableMinor" => "0.3", "releaseTag" => "0.3.0", "releaseCommit" => SHA,
        "publication" => { "manualVersion" => "0.3", "sourceRoot" => "docs/manual", "locales" => %w[en ko], "contentStatus" => content_status },
        "overview" => { "documents" => documents }, "evidence" => ManualDocs::Validator::PINNED_EVIDENCE.map(&:dup),
        "modules" => [row],
      }
      manifest_path = File.join(root, "docs/manual/manifest.yaml")
      File.write(manifest_path, YAML.dump(manifest))
      inventory_path = File.join(root, "inventory.json")
      File.write(inventory_path, JSON.generate([row.slice("gradlePath", "projectName", "sourceDir", "kind")]))
      result = ManualDocs::ReleaseContract::ValidationResult.new(
        errors: release_errors, checked_count: 0, source_path_count: 1, evidence_path_count: 1,
      )
      yield ManualDocs::ReleaseManualValidator.new(
        repository_root: root, tag: "0.3.0", expected_sha: SHA,
        inventory_path: inventory_path, manifest_path: manifest_path,
        release_contract: Struct.new(:validate).new(result), allow_planned: allow_planned,
      )
    end
  end
end
