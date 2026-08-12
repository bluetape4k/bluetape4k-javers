require "minitest/autorun"
require "tmpdir"

require_relative "bom_contract"

class BomContractTest < Minitest::Test
  GROUP = "io.github.bluetape4k.javers"
  MODULES = %w[
    javers-core
    javers-ddd
    javers-exposed
    javers-persistence-kafka
    javers-persistence-redis
    javers-spring-boot4-autoconfigure
  ].freeze

  def contract(pom_artifacts: MODULES, en_modules: MODULES, ko_modules: en_modules)
    Dir.mktmpdir("bom-contract") do |root|
      pom = File.join(root, "pom.xml")
      en = File.join(root, "bom-en.md")
      ko = File.join(root, "bom-ko.md")
      File.write(pom, pom_xml(pom_artifacts))
      File.write(en, manual(en_modules))
      File.write(ko, manual(ko_modules))
      yield Publication::BomContract.new(
        pom_path: pom,
        manual_paths: { "en" => en, "ko" => ko },
        group_id: GROUP,
      )
    end
  end

  def test_accepts_the_generated_six_module_bom_and_matching_locales
    contract { |checker| assert_empty checker.errors }
  end

  def test_rejects_manual_module_drift
    contract(en_modules: MODULES - ["javers-spring-boot4-autoconfigure"]) do |checker|
      assert checker.errors.any? { |error| error.include?("en manual modules differ") }
    end
  end

  def test_rejects_non_publishable_example_or_benchmark_constraints
    contract(pom_artifacts: MODULES + ["examples-javers-exposed-ddd", "benchmark-javers-exposed-benchmark"]) do |checker|
      assert checker.errors.any? { |error| error.include?("non-publishable") }
    end
  end

  def test_rejects_locale_parity_drift
    contract(ko_modules: MODULES - ["javers-persistence-kafka"]) do |checker|
      assert checker.errors.any? { |error| error.include?("locale module sets differ") }
    end
  end

  private

  def pom_xml(artifacts)
    dependencies = artifacts.map do |artifact|
      <<~XML
        <dependency>
          <groupId>#{GROUP}</groupId>
          <artifactId>#{artifact}</artifactId>
          <version>0.4.0</version>
        </dependency>
      XML
    end.join
    <<~XML
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>#{GROUP}</groupId>
        <artifactId>bluetape4k-javers-bom</artifactId>
        <version>0.4.0</version>
        <dependencyManagement><dependencies>
          #{dependencies}
        </dependencies></dependencyManagement>
      </project>
    XML
  end

  def manual(modules)
    <<~MARKDOWN
      # Javers BOM

      <!-- BOM_PUBLISHED_MODULES:START -->
      #{modules.map { |mod| "`#{mod}`" }.join(", ")}
      <!-- BOM_PUBLISHED_MODULES:END -->
    MARKDOWN
  end
end
