require "pathname"
require "yaml"

module ManualDocs
  SUPPORTED_SCHEMA_VERSION = 2
  VALID_KINDS = %w[library bom example].freeze
  VALID_GROUPS = %w[foundation persistence examples].freeze

  class Validator
    REQUIRED_FIELDS = %w[id gradlePath projectName sourceDir kind group artifact status sourcePaths].freeze
    LOCALES = { "en" => "English", "ko" => "Korean" }.freeze
    PINNED_EVIDENCE = [{
      "id" => "javers-exposed-ddd-envers-comparison",
      "kind" => "benchmark",
      "path" => "docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json",
    }].freeze
    attr_reader :errors

    def initialize(inventory:, manifest_path:, repository_root:, expected_release:, strict: false)
      @inventory = inventory
      @manifest_path = File.expand_path(manifest_path)
      @repository_root = File.expand_path(repository_root)
      @expected_release = expected_release
      @strict = strict
      @errors = validate.sort
    end

    private

    def validate
      return ["manual manifest not found"] unless File.file?(@manifest_path)
      manifest = YAML.safe_load(File.read(@manifest_path))
      return ["manual manifest must be a mapping"] unless manifest.is_a?(Hash)
      errors = validate_header(manifest)
      @content_complete = manifest.dig("publication", "contentStatus") == "complete"
      modules = manifest["modules"]
      return errors << "manual manifest modules must be an array" unless modules.is_a?(Array)
      entries = modules.select { |entry| entry.is_a?(Hash) }
      errors.concat(duplicates(entries, "id").map { |id| "duplicate id #{id}" })
      errors.concat(duplicates(entries, "gradlePath").map { |path| "duplicate gradlePath #{path}" })
      errors.concat(validate_inventory(entries))
      entries.each { |entry| errors.concat(validate_entry(entry)) }
      errors.concat(validate_evidence(manifest.fetch("evidence", [])))
      errors.concat(validate_overview(manifest, entries))
      errors
    rescue Psych::SyntaxError => error
      ["manual manifest YAML is invalid: #{error.problem}"]
    end

    def validate_header(manifest)
      errors = []
      errors << "manual manifest schemaVersion must be #{SUPPORTED_SCHEMA_VERSION}" unless manifest["schemaVersion"] == SUPPORTED_SCHEMA_VERSION
      errors << "manual manifest repository must be bluetape4k-javers" unless manifest["repository"] == "bluetape4k-javers"
      errors << "manual manifest releaseRef must be #{@expected_release.fetch('ref')}" unless manifest["releaseRef"] == @expected_release.fetch("ref")
      errors << "manual manifest stableVersion must be #{@expected_release.fetch('ref')}" unless manifest["stableVersion"] == @expected_release.fetch("ref")
      errors << "manual manifest stableMinor must be 0.2" unless manifest["stableMinor"] == "0.2"
      errors << "manual manifest releaseTag must be #{@expected_release.fetch('ref')}" unless manifest["releaseTag"] == @expected_release.fetch("ref")
      errors << "manual manifest releaseCommit must be #{@expected_release.fetch('commit')}" unless manifest["releaseCommit"] == @expected_release.fetch("commit")
      publication = manifest["publication"]
      return errors << "manual publication must be a mapping" unless publication.is_a?(Hash)
      errors << "manual publication locales must be en and ko" unless publication["locales"] == %w[en ko]
      if @strict
        errors << "manual publication manualVersion must be 0.2" unless publication["manualVersion"] == "0.2"
        errors << "manual publication sourceRoot must be docs/manual" unless publication["sourceRoot"] == "docs/manual"
        errors << "manual publication contentStatus must be planned or complete" unless %w[planned complete].include?(publication["contentStatus"])
      end
      errors
    end

    def validate_inventory(entries)
      return ["module inventory must be an array"] unless @inventory.is_a?(Array)
      errors = duplicates(@inventory, "gradlePath").map { |path| "inventory: duplicate gradlePath #{path}" }
      by_inventory = @inventory.grep(Hash).to_h { |row| [row["gradlePath"], row] }
      by_manifest = entries.to_h { |row| [row["gradlePath"], row] }
      (by_inventory.keys - by_manifest.keys).sort.each { |path| errors << "#{path}: missing from manifest" }
      (by_manifest.keys - by_inventory.keys).sort.each { |path| errors << "#{path}: not present in inventory" }
      entries.each do |entry|
        row = by_inventory[entry["gradlePath"]]
        next unless row
        %w[projectName sourceDir kind].each do |field|
          errors << "#{entry['id']}: #{field} does not match inventory" unless entry[field] == row[field]
        end
      end
      errors
    end

    def validate_entry(entry)
      errors = []
      REQUIRED_FIELDS.each do |field|
        missing = !entry.key?(field) || (field == "group" && entry[field].nil?)
        errors << "#{entry['id'] || 'module'}: missing manifest field #{field}" if missing
      end
      errors << "#{entry['id']}: invalid kind #{entry['kind'].inspect}" unless VALID_KINDS.include?(entry["kind"])
      errors << "#{entry['id']}: invalid group #{entry['group'].inspect}" if !entry["group"].nil? && !VALID_GROUPS.include?(entry["group"])
      errors << "#{entry['id']}: published artifact must be present" if %w[library bom].include?(entry["kind"]) && blank?(entry["artifact"])
      errors << "#{entry['id']}: example artifact must be null" if entry["kind"] == "example" && !entry["artifact"].nil?
      errors.concat(validate_paths(entry, "sourcePaths")) if entry.key?("sourcePaths")
      errors.concat(validate_routes(entry)) if @content_complete || LOCALES.keys.any? { |locale| entry.key?(locale) }
      errors
    end

    def validate_evidence(evidence)
      return ["manual evidence must be an array"] unless evidence.is_a?(Array)
      errors = duplicates(evidence, "id").map { |id| "duplicate evidence id #{id}" }
      if @strict && evidence != PINNED_EVIDENCE
        errors << "manual evidence must contain exactly the pinned benchmark artifact"
      end
      evidence.each do |entry|
        unless entry.is_a?(Hash) && entry["id"].is_a?(String) && entry["kind"] == "benchmark" && entry["path"].is_a?(String)
          errors << "manual evidence entry must contain id, benchmark kind, and path"
          next
        end
        absolute = File.expand_path(entry["path"], @repository_root)
        unless safe_relative?(entry["path"]) && within?(absolute, @repository_root) && File.file?(absolute)
          errors << "#{entry['id']}: missing evidence path #{entry['path']}"
        end
      end
      errors
    end

    def validate_overview(manifest, entries)
      overview = manifest["overview"]
      complete = manifest.dig("publication", "contentStatus") == "complete"
      return complete ? ["manual manifest overview must be a mapping"] : [] unless overview.is_a?(Hash)
      documents = overview["documents"]
      return ["manual overview documents must be a mapping"] unless documents.is_a?(Hash)
      errors = []
      locale_paths = {}
      LOCALES.each do |locale, language|
        paths = documents[locale]
        unless paths.is_a?(Array)
          errors << "manual overview #{language} documents must be an array"
          next
        end
        locale_paths[locale] = paths
        paths.each do |path|
          unless safe_relative?(path) && path.start_with?("#{locale}/")
            errors << "manual overview unsafe #{language} document #{path}"
            next
          end
          absolute = File.expand_path(path, File.dirname(@manifest_path))
          if manual_file?(absolute)
            errors.concat(validate_document_references(absolute, "overview"))
          elsif within?(absolute, File.dirname(@manifest_path)) && File.file?(absolute)
            errors << "manual overview unsafe #{language} document realpath #{path}"
          else
            errors << "manual overview missing #{language} document #{path}"
          end
        end
      end
      if locale_paths.keys.sort == LOCALES.keys.sort
        en_slugs = locale_paths.fetch("en").map { |path| path.delete_prefix("en/") }
        ko_slugs = locale_paths.fetch("ko").map { |path| path.delete_prefix("ko/") }
        errors << "manual overview English/Korean routes differ" unless en_slugs == ko_slugs
      end
      if complete && @strict
        LOCALES.keys.each do |locale|
          registered = Array(locale_paths[locale]) + entries.map { |entry| entry[locale] }.compact
          actual = Dir.glob(File.join(File.dirname(@manifest_path), locale, "**/*.md")).map do |path|
            Pathname.new(path).relative_path_from(Pathname.new(File.dirname(@manifest_path))).to_s
          end
          (actual - registered).sort.each { |path| errors << "unregistered manual document #{path}" }
          (registered - actual).sort.each { |path| errors << "registered manual document not found #{path}" }
        end
      end
      errors
    end

    def validate_routes(entry)
      errors = []
      LOCALES.each do |locale, language|
        path = entry[locale]
        if blank?(path)
          errors << "#{entry['id']}: missing #{language} route"
          next
        end
        unless safe_relative?(path) && path.start_with?("#{locale}/")
          errors << "#{entry['id']}: unsafe #{language} route"
          next
        end
        absolute = File.expand_path(path, File.dirname(@manifest_path))
        if manual_file?(absolute)
          errors.concat(validate_document_references(absolute, entry["id"]))
        elsif within?(absolute, File.dirname(@manifest_path)) && File.file?(absolute)
          errors << "#{entry['id']}: unsafe #{language} document realpath"
        else
          errors << "#{entry['id']}: missing #{language} document"
        end
      end
      if entry["en"].is_a?(String) && entry["ko"].is_a?(String)
        errors << "#{entry['id']}: English/Korean route differs" unless entry["en"].delete_prefix("en/") == entry["ko"].delete_prefix("ko/")
      end
      errors
    end

    def validate_paths(entry, field)
      paths = entry[field]
      return ["#{entry['id']}: #{field} must be an array"] unless paths.is_a?(Array)
      return ["#{entry['id']}: #{field} must contain non-empty strings"] unless paths.all? { |path| path.is_a?(String) && !path.empty? }
      paths.each_with_object([]) do |path, errors|
        absolute = File.expand_path(path, @repository_root)
        unless safe_relative?(path) && within?(absolute, @repository_root) && File.exist?(absolute) &&
               within?(File.realpath(absolute), File.realpath(@repository_root))
          errors << "#{entry['id']}: missing #{field} path #{path}"
        end
      end
    end

    def validate_document_references(document, label)
      content = File.read(document)
      inline = content.scan(/!?\[[^\]]*\]\(([^)]+)\)/).flatten
      references = content.scan(/^[ \t]{0,3}\[[^\]]+\]:[ \t]*(?:<([^>\r\n]+)>|([^ \t\r\n]+))/).map { |match| match.compact.first }
      html = content.scan(/<(?:a|img)\b[^>]*(?:href|src)=["']([^"']+)["']/i).flatten
      (inline + references + html).each_with_object([]) do |raw, errors|
        target = raw.to_s.strip.split(/\s+["']/, 2).first.to_s.delete_prefix("<").delete_suffix(">").split(/[?#]/, 2).first
        next if target.empty? || target.start_with?("#", "/") || target.match?(/\A[a-z][a-z0-9+.-]*:/i)
        absolute = File.expand_path(target, File.dirname(document))
        unless within?(absolute, @repository_root) && File.exist?(absolute) &&
               within?(File.realpath(absolute), File.realpath(@repository_root))
          errors << "#{label}: missing or unsafe document reference #{raw}"
        end
      end
    end

    def duplicates(rows, field)
      rows.grep(Hash).group_by { |row| row[field] }.select { |value, matches| value && matches.length > 1 }.keys
    end

    def manual_file?(path)
      manual_root = File.dirname(@manifest_path)
      within?(path, manual_root) && File.file?(path) && within?(File.realpath(path), File.realpath(manual_root))
    end

    def safe_relative?(value)
      value.is_a?(String) && !value.empty? && !Pathname.new(value).absolute? && Pathname.new(value).each_filename.none? { |part| part == ".." }
    end

    def within?(path, root)
      path == root || path.start_with?(root + File::SEPARATOR)
    end

    def blank?(value)
      value.nil? || (value.respond_to?(:empty?) && value.empty?)
    end
  end
end
