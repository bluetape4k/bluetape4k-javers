#!/usr/bin/env ruby
# frozen_string_literal: true

require "pathname"
require "yaml"

module CurrentManual
  class ContractError < StandardError; end

  class SourceInventory
    Project = Struct.new(:gradle_path, :project_name, :source_dir, keyword_init: true)

    def initialize(repository_root:)
      @repository_root = Pathname.new(repository_root).realpath
      @settings_path = @repository_root.join("settings.gradle.kts")
    end

    def projects
      text = @settings_path.read
      names = text.scan(/include\((.*?)\)/m).flat_map { |match| match.first.scan(/"([^"]+)"/).flatten }
      source_dirs = text.scan(/project\(\":([^\"]+)\"\)\.projectDir\s*=\s*file\(\"([^\"]+)\"\)/)
        .to_h { |name, path| [name, path] }
      names.uniq.sort.map do |name|
        Project.new(
          gradle_path: ":#{name}",
          project_name: name,
          source_dir: source_dirs.fetch(name, name),
        )
      end
    end
  end

  class Validator
    LOCALES = %w[en ko].freeze
    REQUIRED_MODULE_FIELDS = %w[id title gradlePath projectName sourceDir kind group artifact status sourcePaths en ko].freeze
    CURRENT_KINDS = %w[library bom example benchmark].freeze
    LINK_PATTERNS = [
      /!?\[[^\]]*\]\(([^)]+)\)/,
      /^[ \t]{0,3}\[[^\]]+\]:[ \t]*(?:<([^>\r\n]+)>|([^ \t\r\n]+))/,
      /<(?:a|img)\b[^>]*(?:href|src)=["']([^"']+)["']/i,
      /<((?:https?:\/\/)[^ >]+)>/i,
    ].freeze

    attr_reader :errors

    def initialize(repository_root:, manifest_path: "docs/manual/current/manifest.yaml", expected_version: nil)
      @repository_root = Pathname.new(repository_root).realpath
      @current_root = @repository_root.join("docs/manual/current")
      @manifest_path = @repository_root.join(manifest_path)
      @expected_version = normalize_version(expected_version || base_version)
      @errors = validate.sort
    end

    def valid?
      errors.empty?
    end

    private

    def validate
      return ["current manual manifest not found: #{@manifest_path}"] unless @manifest_path.file?

      manifest = YAML.safe_load(@manifest_path.read)
      return ["current manual manifest must be a mapping"] unless manifest.is_a?(Hash)

      errors = validate_header(manifest)
      inventory = SourceInventory.new(repository_root: @repository_root).projects
      modules = manifest["modules"]
      return errors << "current manual modules must be an array" unless modules.is_a?(Array)

      entries = modules.grep(Hash)
      errors.concat(duplicate_errors(entries, "id", "manifest"))
      errors.concat(duplicate_errors(entries, "projectName", "manifest"))
      errors.concat(duplicate_errors(entries, "gradlePath", "manifest"))
      errors.concat(validate_inventory(entries, inventory))
      entries.each { |entry| errors.concat(validate_module(entry)) }
      errors.concat(validate_overview(manifest, entries))
      errors.concat(validate_registered_documents(manifest, entries))
      errors.concat(validate_relative_links(manifest, entries))
      errors.concat(validate_index_content(entries))
      errors.concat(validate_changelog)
      errors
    rescue Psych::SyntaxError => error
      ["current manual manifest YAML is invalid: #{error.problem}"]
    rescue Errno::ENOENT => error
      ["current manual source file is missing: #{error.message}"]
    end

    def validate_header(manifest)
      errors = []
      errors << "current manual schemaVersion must be 1" unless manifest["schemaVersion"] == 1
      errors << "current manual repository must be bluetape4k-javers" unless manifest["repository"] == "bluetape4k-javers"
      errors << "current manual version must be #{@expected_version}" unless manifest["version"] == @expected_version
      publication = manifest["publication"]
      unless publication.is_a?(Hash)
        return errors << "current manual publication must be a mapping"
      end
      errors << "current manual publication manualVersion must be #{@expected_version.split(".")[0, 2].join(".")}" unless publication["manualVersion"] == @expected_version.split(".")[0, 2].join(".")
      errors << "current manual publication sourceRoot must be docs/manual/current" unless publication["sourceRoot"] == "docs/manual/current"
      errors << "current manual publication locales must be en and ko" unless publication["locales"] == LOCALES
      errors << "current manual publication contentStatus must be complete" unless publication["contentStatus"] == "complete"
      errors
    end

    def validate_inventory(entries, inventory)
      errors = []
      expected_paths = inventory.map(&:gradle_path)
      actual_paths = entries.map { |entry| entry["gradlePath"] }
      (expected_paths - actual_paths).sort.each { |path| errors << "#{path}: missing from current manual manifest" }
      (actual_paths - expected_paths).sort.each { |path| errors << "#{path}: not present in current source settings" }

      by_inventory = inventory.to_h { |project| [project.gradle_path, project] }
      entries.each do |entry|
        project = by_inventory[entry["gradlePath"]]
        next unless project

        errors << "#{entry['id']}: projectName does not match settings.gradle.kts" unless entry["projectName"] == project.project_name
        errors << "#{entry['id']}: sourceDir does not match settings.gradle.kts" unless entry["sourceDir"] == project.source_dir
        expected_kind = kind_for(project)
        errors << "#{entry['id']}: kind must be #{expected_kind} for the current source project" unless entry["kind"] == expected_kind
        source = @repository_root.join(project.source_dir)
        errors << "#{entry['id']}: sourceDir does not exist: #{project.source_dir}" unless source.directory?
        errors << "#{entry['id']}: sourceDir has no build.gradle.kts" unless source.join("build.gradle.kts").file?
      end
      errors
    end

    def validate_module(entry)
      errors = []
      id = entry["id"] || "module"
      REQUIRED_MODULE_FIELDS.each do |field|
        errors << "#{id}: missing #{field}" unless entry.key?(field)
      end
      return errors unless REQUIRED_MODULE_FIELDS.all? { |field| entry.key?(field) }

      errors << "#{id}: id must equal projectName" unless entry["id"] == entry["projectName"]
      errors << "#{id}: invalid kind #{entry['kind'].inspect}" unless CURRENT_KINDS.include?(entry["kind"])
      errors << "#{id}: sourcePaths must equal [sourceDir]" unless entry["sourcePaths"] == [entry["sourceDir"]]
      errors << "#{id}: published artifact must be present" if %w[library bom].include?(entry["kind"]) && blank?(entry["artifact"])
      errors << "#{id}: non-published artifact must be null" if %w[example benchmark].include?(entry["kind"]) && !entry["artifact"].nil?
      if %w[library bom].include?(entry["kind"])
        expected_artifact = "io.github.bluetape4k.javers:#{entry['projectName']}"
        errors << "#{id}: artifact does not match projectName" unless entry["artifact"] == expected_artifact
      end
      errors.concat(validate_title(id, entry["title"]))
      LOCALES.each do |locale|
        route = entry[locale]
        errors << "#{id}: missing #{locale} route" unless route.is_a?(String) && !route.empty?
        next unless route.is_a?(String) && !route.empty?

        errors << "#{id}: unsafe #{locale} route" unless safe_route?(route, locale)
        errors << "#{id}: missing or unsafe #{locale} document #{route}" unless safe_document?(route, locale)
      end
      if entry["en"].is_a?(String) && entry["ko"].is_a?(String)
        errors << "#{id}: English/Korean route differs" unless entry["en"].delete_prefix("en/") == entry["ko"].delete_prefix("ko/")
      end
      errors
    end

    def kind_for(project)
      return "bom" if project.project_name == "bluetape4k-javers-bom"
      return "benchmark" if project.project_name.start_with?("benchmark-") || project.source_dir.start_with?("benchmark/")
      return "example" if project.project_name.start_with?("examples-") || project.source_dir.start_with?("examples/")

      "library"
    end

    def validate_title(id, title)
      return ["#{id}: title must contain en and ko"] unless title.is_a?(Hash) && LOCALES.all? { |locale| title[locale].is_a?(String) && !title[locale].empty? }

      []
    end

    def validate_overview(manifest, entries)
      overview = manifest["overview"]
      return ["current manual overview must be a mapping"] unless overview.is_a?(Hash)

      documents = overview["documents"]
      return ["current manual overview documents must be a mapping"] unless documents.is_a?(Hash)

      errors = []
      LOCALES.each do |locale|
        paths = documents[locale]
        unless paths.is_a?(Array) && !paths.empty?
          errors << "current manual overview #{locale} documents must be a non-empty array"
          next
        end
        paths.each do |path|
          errors << "current manual overview unsafe #{locale} document #{path}" unless safe_route?(path, locale)
          errors << "current manual overview missing or unsafe #{locale} document #{path}" unless safe_document?(path, locale)
        end
      end
      en_routes = Array(documents["en"]).map { |path| path.delete_prefix("en/") }
      ko_routes = Array(documents["ko"]).map { |path| path.delete_prefix("ko/") }
      errors << "current manual overview English/Korean routes differ" unless en_routes == ko_routes
      errors << "current manual overview must include en/index.md and ko/index.md" unless documents["en"]&.include?("en/index.md") && documents["ko"]&.include?("ko/index.md")
      errors
    end

    def validate_registered_documents(manifest, entries)
      overview = manifest.fetch("overview", {}).fetch("documents", {})
      registered = LOCALES.to_h do |locale|
        pages = Array(overview[locale]) + entries.map { |entry| entry[locale] }
        [locale, pages.compact.uniq.sort]
      end
      errors = []
      LOCALES.each do |locale|
        actual = Dir.glob(@current_root.join(locale, "**/*.md").to_s).map do |path|
          Pathname.new(path).relative_path_from(@current_root).to_s
        end.sort
        actual.each do |path|
          errors << "unsafe current manual document #{path}" unless safe_document?(path, locale)
        end
        errors.concat((actual - registered.fetch(locale)).map { |path| "unregistered current manual document #{path}" })
        errors.concat((registered.fetch(locale) - actual).map { |path| "registered current manual document not found #{path}" })
      end
      errors
    end

    def validate_relative_links(manifest, entries)
      overview = manifest.fetch("overview", {}).fetch("documents", {})
      pages = LOCALES.flat_map { |locale| Array(overview[locale]) } + entries.flat_map { |entry| LOCALES.map { |locale| entry[locale] } }
      pages.compact.uniq.flat_map do |relative|
        file = @current_root.join(relative)
        next [] unless file.file?

        extract_links(file.read).each_with_object([]) do |target, failures|
          target = target.strip.split(/\s+['"]/, 2).first.delete_prefix("<").delete_suffix(">").split(/[?#]/, 2).first
          next if target.empty? || target.start_with?("#", "/") || target.match?(%r{\A[a-z][a-z0-9+.-]*:}i)

          resolved = file.dirname.join(target).cleanpath
          next if resolved.file? && resolved.realpath.to_s.start_with?(@current_root.realpath.to_s + File::SEPARATOR)

          failures << "#{relative}: missing or unsafe document reference #{target}"
        end
      end
    end

    def validate_changelog
      changelog = @repository_root.join("CHANGELOG.md")
      return ["CHANGELOG.md not found"] unless changelog.file?

      content = changelog.read
      errors = []
      errors << "CHANGELOG.md must contain an unreleased section" unless content.match?(/^## \[미공개\]/)
      %w[#334 #341 #333 #335 #338 #336 #337 #339 #342 #340].each do |issue|
        errors << "CHANGELOG.md missing post-0.3 issue #{issue}" unless content.include?(issue)
      end
      errors
    end

    def validate_index_content(entries)
      entries_by_locale = LOCALES.to_h do |locale|
        path = @current_root.join(locale, "index.md")
        [locale, path.file? ? path.read : ""]
      end
      errors = []
      LOCALES.each do |locale|
        content = entries_by_locale.fetch(locale)
        errors << "current manual #{locale}/index.md must mention #{@expected_version}" unless content.include?(@expected_version)
        entries.each do |entry|
          project_name = entry["projectName"]
          errors << "current manual #{locale}/index.md missing #{project_name}" unless content.include?(project_name)
        end
      end
      errors
    end

    def duplicate_errors(entries, field, scope)
      entries.group_by { |entry| entry[field] }.select { |value, rows| value && rows.length > 1 }.keys.map do |value|
        "duplicate #{scope} #{field} #{value}"
      end
    end

    def safe_route?(path, locale)
      path.is_a?(String) && path.start_with?("#{locale}/") && !Pathname.new(path).absolute? && Pathname.new(path).each_filename.none? { |part| part == ".." }
    end

    def safe_document?(path, locale)
      return false unless safe_route?(path, locale)

      document = @current_root.join(path)
      document.file? && within_current_root?(document.realpath)
    rescue Errno::ENOENT, Errno::EACCES
      false
    end

    def within_current_root?(path)
      root = @current_root.realpath.to_s
      candidate = path.to_s
      candidate == root || candidate.start_with?(root + File::SEPARATOR)
    end

    def extract_links(content)
      LINK_PATTERNS.flat_map do |pattern|
        content.to_enum(:scan, pattern).map { Regexp.last_match.captures.compact.first }
      end.uniq
    end

    def normalize_version(version)
      version.to_s.sub(/-[A-Za-z0-9.]+\z/, "")
    end

    def blank?(value)
      value.nil? || (value.respond_to?(:empty?) && value.empty?)
    end

    def base_version
      line = @repository_root.join("gradle.properties").read.lines.find { |entry| entry.start_with?("baseVersion=") }
      line&.split("=", 2)&.last&.strip || raise(ContractError, "baseVersion is missing from gradle.properties")
    end
  end
end

if $PROGRAM_NAME == __FILE__
  expected_version = nil
  arguments = ARGV.dup
  if arguments.first == "--version"
    arguments.shift
    expected_version = arguments.shift
  end
  abort("usage: ruby scripts/manual/current_manual_contract.rb [--version VERSION]") unless arguments.empty? && (expected_version.nil? || !expected_version.empty?)

  validator = CurrentManual::Validator.new(repository_root: Dir.pwd, expected_version: expected_version)
  abort(validator.errors.join("\n")) unless validator.valid?

  puts "Current manual contract valid: version=#{expected_version || 'baseVersion'} modules=#{CurrentManual::SourceInventory.new(repository_root: Dir.pwd).projects.length} locales=en,ko"
end
