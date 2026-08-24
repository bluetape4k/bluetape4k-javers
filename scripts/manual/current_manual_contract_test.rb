#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "current_manual_contract"

class CurrentManualContractTest < Minitest::Test
  def test_repository_current_manual_matches_settings_and_changelog
    validator = CurrentManual::Validator.new(repository_root: File.expand_path("../..", __dir__))

    assert_empty validator.errors
  end

  def test_rejects_release_version_drift
    validator = CurrentManual::Validator.new(
      repository_root: File.expand_path("../..", __dir__),
      expected_version: "9.9.9",
    )

    assert_includes validator.errors, "current manual version must be 9.9.9"
  end

  def test_rejects_missing_locale_document
    with_fixture do |root|
      FileUtils.rm(root.join("docs/manual/current/ko/modules/javers-core.md"))

      validator = CurrentManual::Validator.new(repository_root: root.to_s)

      assert validator.errors.any? { |error| error.include?("missing ko document") }
    end
  end

  private

  def with_fixture
    Dir.mktmpdir("current-manual") do |directory|
      root = Pathname.new(directory)
      File.write(root.join("gradle.properties"), "baseVersion=1.0.0\n")
      File.write(root.join("settings.gradle.kts"), <<~KOTLIN)
        include("javers-core")
      KOTLIN
      FileUtils.mkdir_p(root.join("javers-core"))
      File.write(root.join("javers-core/build.gradle.kts"), "plugins { base }\n")
      File.write(root.join("CHANGELOG.md"), "## [미공개]\n#334 #333 #335 #336 #337 #338 #339 #340 #341 #342\n")

      current = root.join("docs/manual/current")
      FileUtils.mkdir_p(current.join("en/modules"))
      FileUtils.mkdir_p(current.join("ko/modules"))
      File.write(current.join("en/index.md"), "# 1.0.0\n[javers-core](modules/javers-core.md)\n")
      File.write(current.join("ko/index.md"), "# 1.0.0\n[javers-core](modules/javers-core.md)\n")
      File.write(current.join("en/modules/javers-core.md"), "# core\n")
      File.write(current.join("ko/modules/javers-core.md"), "# core\n")
      File.write(current.join("manifest.yaml"), YAML.dump(manifest))

      yield root
    end
  end

  def manifest
    {
      "schemaVersion" => 1,
      "repository" => "bluetape4k-javers",
      "version" => "1.0.0",
      "publication" => {
        "manualVersion" => "1.0",
        "sourceRoot" => "docs/manual/current",
        "locales" => %w[en ko],
        "contentStatus" => "complete",
      },
      "overview" => { "documents" => { "en" => ["en/index.md"], "ko" => ["ko/index.md"] } },
      "modules" => [{
        "id" => "javers-core",
        "title" => { "en" => "JaVers core", "ko" => "JaVers core" },
        "gradlePath" => ":javers-core",
        "projectName" => "javers-core",
        "sourceDir" => "javers-core",
        "kind" => "library",
        "group" => "foundation",
        "artifact" => "io.github.bluetape4k.javers:javers-core",
        "status" => "current",
        "sourcePaths" => ["javers-core"],
        "en" => "en/modules/javers-core.md",
        "ko" => "ko/modules/javers-core.md",
      }],
    }
  end
end
