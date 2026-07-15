require "fileutils"
require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "manual_contract"

class ManualContractTest < Minitest::Test
  RELEASE = { "ref" => "0.2.1", "commit" => "bffe19439ca891fa5301a76421bdef7ba75252a0" }.freeze

  def validate(module_overrides = {}, manifest_overrides = {})
    Dir.mktmpdir do |root|
      source = File.join(root, "javers-core")
      FileUtils.mkdir_p(source)
      File.write(File.join(source, "build.gradle.kts"), "")
      evidence_path = File.join(root, "docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json")
      FileUtils.mkdir_p(File.dirname(evidence_path))
      File.write(evidence_path, "{}\n")
      row = {
        "id" => "javers-core", "gradlePath" => ":javers-core", "projectName" => "javers-core",
        "sourceDir" => "javers-core", "kind" => "library", "group" => "foundation",
        "artifact" => "io.github.bluetape4k.javers:javers-core", "status" => "stable",
        "sourcePaths" => ["javers-core"],
      }.merge(module_overrides)
      manifest = {
        "schemaVersion" => 2, "repository" => "bluetape4k-javers", "releaseRef" => RELEASE["ref"],
        "stableVersion" => RELEASE["ref"], "stableMinor" => "0.2", "releaseTag" => RELEASE["ref"],
        "releaseCommit" => RELEASE["commit"],
        "publication" => { "manualVersion" => "0.2", "sourceRoot" => "docs/manual", "locales" => %w[en ko], "contentStatus" => "planned" },
        "evidence" => ManualDocs::Validator::PINNED_EVIDENCE.map(&:dup), "modules" => [row],
      }.merge(manifest_overrides)
      path = File.join(root, "docs/manual/manifest.yaml")
      FileUtils.mkdir_p(File.dirname(path))
      File.write(path, YAML.dump(manifest))
      inventory = [row.slice("gradlePath", "projectName", "sourceDir", "kind")]
      yield ManualDocs::Validator.new(
        inventory: inventory, manifest_path: path, repository_root: root, expected_release: RELEASE, strict: true,
      )
    end
  end

  def test_accepts_planned_inventory_without_content_routes
    validate { |validator| assert_empty validator.errors }
  end

  def test_rejects_missing_locale_route_when_routes_are_declared
    validate("en" => "en/modules/javers-core.md") do |validator|
      assert validator.errors.any? { |error| error.include?("missing Korean route") }
    end
  end

  def test_requires_both_locale_routes_when_content_is_complete
    validate({}, "publication" => {
      "manualVersion" => "0.2", "sourceRoot" => "docs/manual", "locales" => %w[en ko], "contentStatus" => "complete",
    }) do |validator|
      assert validator.errors.any? { |error| error.include?("missing English route") }
      assert validator.errors.any? { |error| error.include?("missing Korean route") }
    end
  end

  def test_rejects_missing_document_reference
    validate do |validator|
      root = validator.instance_variable_get(:@repository_root)
      manifest_path = validator.instance_variable_get(:@manifest_path)
      %w[en ko].each do |locale|
        document = File.join(root, "docs/manual/#{locale}/modules/javers-core.md")
        FileUtils.mkdir_p(File.dirname(document))
        File.write(document, "[missing](missing.md)\n")
      end
      manifest = YAML.safe_load(File.read(manifest_path))
      manifest.fetch("modules").first.merge!(
        "en" => "en/modules/javers-core.md", "ko" => "ko/modules/javers-core.md",
      )
      File.write(manifest_path, YAML.dump(manifest))
      validator = ManualDocs::Validator.new(
        inventory: validator.instance_variable_get(:@inventory), manifest_path: manifest_path,
        repository_root: root, expected_release: RELEASE, strict: true,
      )
      assert validator.errors.any? { |error| error.include?("missing or unsafe document reference") }
    end
  end

  def test_rejects_missing_source_file
    validate("sourcePaths" => ["javers-core/Missing.kt"]) do |validator|
      assert validator.errors.any? { |error| error.include?("missing sourcePaths path") }
    end
  end

  def test_rejects_release_metadata_other_than_the_pinned_release
    validate({}, "releaseRef" => "0.2.0", "releaseCommit" => "a" * 40) do |validator|
      assert validator.errors.any? { |error| error.include?("releaseRef must be 0.2.1") }
      assert validator.errors.any? { |error| error.include?("releaseCommit must be #{RELEASE['commit']}") }
    end
  end

  def test_requires_the_single_pinned_benchmark_evidence
    validate({}, "evidence" => []) do |validator|
      assert validator.errors.any? { |error| error.include?("exactly the pinned benchmark artifact") }
    end
    validate({}, "evidence" => [{ "id" => "other", "kind" => "benchmark", "path" => "docs/benchmark/other.json" }]) do |validator|
      assert validator.errors.any? { |error| error.include?("exactly the pinned benchmark artifact") }
    end
  end

  def test_rejects_missing_reference_style_document_link
    validate do |validator|
      root = validator.instance_variable_get(:@repository_root)
      manifest_path = validator.instance_variable_get(:@manifest_path)
      %w[en ko].each do |locale|
        document = File.join(root, "docs/manual/#{locale}/modules/javers-core.md")
        FileUtils.mkdir_p(File.dirname(document))
        File.write(document, "[missing][source]\n\n[source]: missing.md\n")
      end
      manifest = YAML.safe_load(File.read(manifest_path))
      manifest.fetch("modules").first.merge!("en" => "en/modules/javers-core.md", "ko" => "ko/modules/javers-core.md")
      File.write(manifest_path, YAML.dump(manifest))
      validator = ManualDocs::Validator.new(
        inventory: validator.instance_variable_get(:@inventory), manifest_path: manifest_path,
        repository_root: root, expected_release: RELEASE, strict: true,
      )
      assert validator.errors.any? { |error| error.include?("missing or unsafe document reference") }
    end
  end

  def test_rejects_duplicate_gradle_paths_in_inventory
    validate do |validator|
      inventory = validator.instance_variable_get(:@inventory)
      inventory << inventory.first.dup
      validator = ManualDocs::Validator.new(
        inventory: inventory,
        manifest_path: validator.instance_variable_get(:@manifest_path),
        repository_root: validator.instance_variable_get(:@repository_root),
        expected_release: RELEASE,
        strict: true,
      )
      assert validator.errors.any? { |error| error.include?("inventory: duplicate gradlePath") }
    end
  end
end
