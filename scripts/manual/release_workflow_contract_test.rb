#!/usr/bin/env ruby
# frozen_string_literal: true

require "minitest/autorun"
require "fileutils"
require "open3"
require "tmpdir"
require "yaml"

class ReleaseWorkflowContractTest < Minitest::Test
  WORKFLOW_PATH = File.expand_path("../../.github/workflows/release.yml", __dir__)

  def setup
    @workflow = File.read(WORKFLOW_PATH)
  end

  def test_legacy_release_tag_validation_is_explicit_and_fail_closed
    assert_includes @workflow, 'elif [[ "$MANUAL_VERSION" == "0.3.0" ]]'
    assert_includes @workflow, 'manifest.fetch("releaseTag")'
    assert_includes @workflow, 'manifest.fetch("releaseCommit")'
    assert_includes @workflow, 'LEGACY_MANUAL_METADATA="$(ruby -ryaml -e '
    assert_match(/ruby scripts\/manual\/validate_release_manuals\.rb \\\n+\s+"\$LEGACY_MANUAL_TAG" "\$LEGACY_MANUAL_SHA"/, @workflow)
    assert_includes @workflow, "scripts/manual/release_inventory.rb"
    assert_includes @workflow, '"$VERSION" "$TARGET_SHA"'
    assert_includes @workflow, "No supported manual validation contract"
  end

  def test_legacy_coordinates_command_reads_the_pinned_manifest
    with_manifest("releaseTag" => "0.2.1", "releaseCommit" => "b" * 40) do |root|
      output, error, status = Open3.capture3("ruby", "-ryaml", "-e", legacy_coordinates_script, chdir: root)

      assert status.success?, error
      assert_equal "0.2.1\t#{'b' * 40}\n", output
    end
  end

  def test_legacy_coordinates_command_rejects_unpinned_manifest
    with_manifest("releaseTag" => "0.2.1") do |root|
      _output, error, status = Open3.capture3("ruby", "-ryaml", "-e", legacy_coordinates_script, chdir: root)

      refute status.success?
      assert_includes error, "key not found: \"releaseCommit\""
    end
  end

  def test_current_manual_uses_the_stable_version_for_prerelease_tags
    assert_includes @workflow, 'MANUAL_VERSION="${VERSION%%-*}"'
    assert_includes @workflow, 'current_manual_contract.rb --version "$MANUAL_VERSION"'
  end

  def test_release_notes_require_an_exact_changelog_section
    assert_includes @workflow, 'CHANGELOG.md must contain an exact section for [$VERSION]'
    refute_match(/Using fallback notes/, @workflow)
  end

  def test_any_valid_prerelease_suffix_is_published_as_prerelease
    assert_includes @workflow, '[[ "$VERSION" =~ -[A-Za-z0-9.]+$ ]]'
  end

  private

  def legacy_coordinates_script
    match = @workflow.match(/LEGACY_MANUAL_METADATA="\$\(ruby -ryaml -e '(.*?)'\)"/m)
    raise "legacy metadata command is missing" unless match

    match[1]
  end

  def with_manifest(values)
    Dir.mktmpdir("legacy-release-contract") do |directory|
      root = File.join(directory, "repository")
      FileUtils.mkdir_p(File.join(root, "docs/manual"))
      File.write(File.join(root, "docs/manual/manifest.yaml"), YAML.dump(values))
      yield root
    end
  end
end
