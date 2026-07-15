require "json"
require "minitest/autorun"
require "tmpdir"

require_relative "release_inventory"

class ReleaseInventoryTest < Minitest::Test
  SHA = "bffe19439ca891fa5301a76421bdef7ba75252a0"

  def row(path: ":javers-core", source: "javers-core", kind: "library")
    { "gradlePath" => path, "projectName" => path.delete_prefix(":"), "sourceDir" => source, "kind" => kind }
  end

  def runner(type: "tag", sha: SHA, tree: ["javers-core/build.gradle.kts"])
    lambda do |arguments|
      case arguments.first
      when "cat-file" then ["#{type}\n", true]
      when "rev-parse" then ["#{sha}\n", true]
      when "ls-tree" then [tree.join("\n") + "\n", true]
      else ["", false]
      end
    end
  end

  def build(rows, expected_count: rows.length, expected_kinds: { "library" => rows.length }, git_runner: runner,
            expected_projects: rows.to_h { |entry| [entry.fetch("gradlePath"), entry.slice("projectName", "sourceDir", "kind")] })
    Dir.mktmpdir do |root|
      input = File.join(root, "input.json")
      output = File.join(root, "output.json")
      File.write(input, JSON.generate(rows))
      inventory = ManualDocs::ReleaseInventory.new(
        repository_root: root, tag: "0.2.1", expected_sha: SHA,
        inventory_path: input, output_path: output, expected_count: expected_count,
        expected_kinds: expected_kinds, expected_projects: expected_projects, git_runner: git_runner,
      )
      yield inventory, output
    end
  end

  def test_filters_the_current_inventory_against_the_release_tree
    rows = [
      row,
      row(path: ":javers-spring-boot4-autoconfigure", source: "javers-spring-boot4-autoconfigure"),
    ]
    build(rows, expected_count: 1, expected_kinds: { "library" => 1 },
      expected_projects: { ":javers-core" => row.slice("projectName", "sourceDir", "kind") },
      git_runner: runner(tree: ["javers-core/build.gradle.kts"])) do |inventory, output|
      written = inventory.write
      assert_equal [":javers-core"], written.map { |entry| entry.fetch("gradlePath") }
      assert_equal written, JSON.parse(File.read(output))
      assert File.binread(output).end_with?("\n")
    end
  end

  def test_rejects_missing_tag
    build([row], git_runner: ->(_arguments) { ["", false] }) do |inventory, _output|
      assert_match(/tag not found/, assert_raises(ManualDocs::ReleaseInventoryError) { inventory.write }.message)
    end
  end

  def test_rejects_wrong_tag_commit
    build([row], git_runner: runner(sha: "a" * 40)) do |inventory, _output|
      assert_match(/expected #{SHA}/, assert_raises(ManualDocs::ReleaseInventoryError) { inventory.write }.message)
    end
  end

  def test_rejects_unsafe_source_dir
    build([row(source: "../javers-core")], git_runner: runner(tree: ["../javers-core/build.gradle.kts"])) do |inventory, _output|
      assert_match(/unsafe sourceDir/, assert_raises(ManualDocs::ReleaseInventoryError) { inventory.write }.message)
    end
  end

  def test_rejects_duplicate_gradle_paths
    rows = [row, row(source: "other")]
    build(rows, git_runner: runner(tree: ["javers-core/build.gradle.kts", "other/build.gradle.kts"])) do |inventory, _output|
      assert_match(/duplicate gradlePath/, assert_raises(ManualDocs::ReleaseInventoryError) { inventory.write }.message)
    end
  end

  def test_rejects_inventory_other_than_seven_projects
    build([row], expected_count: 7) do |inventory, _output|
      assert_match(/count 1, expected 7/, assert_raises(ManualDocs::ReleaseInventoryError) { inventory.write }.message)
    end
  end

  def test_rejects_classification_other_than_five_one_one
    rows = 7.times.map { |index| row(path: ":library-#{index}", source: "library-#{index}") }
    tree = rows.map { |entry| "#{entry.fetch('sourceDir')}/build.gradle.kts" }
    build(rows, expected_count: 7, expected_kinds: { "library" => 5, "bom" => 1, "example" => 1 }, git_runner: runner(tree: tree)) do |inventory, _output|
      assert_match(/classification/, assert_raises(ManualDocs::ReleaseInventoryError) { inventory.write }.message)
    end
  end

  def test_rejects_substituted_release_project_identity
    rows = [
      row(path: ":bluetape4k-javers-bom", source: "bom", kind: "bom"),
      row,
      row(path: ":javers-ddd", source: "javers-ddd"),
      row(path: ":javers-exposed", source: "javers-exposed"),
      row(path: ":javers-persistence-kafka", source: "javers-persistence-kafka"),
      row(path: ":javers-persistence-redis", source: "javers-persistence-redis"),
      row(path: ":substituted-example", source: "examples/javers-exposed-ddd", kind: "example"),
    ]
    tree = rows.map { |entry| "#{entry.fetch('sourceDir')}/build.gradle.kts" }
    build(rows, expected_count: 7, expected_kinds: { "library" => 5, "bom" => 1, "example" => 1 },
      expected_projects: ManualDocs::ReleaseInventory::EXPECTED_PROJECTS,
      git_runner: runner(tree: tree)) do |inventory, _output|
      assert_match(/project identity/, assert_raises(ManualDocs::ReleaseInventoryError) { inventory.write }.message)
    end
  end
end
