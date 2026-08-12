require "rexml/document"

module Publication
  class BomContract
    DEFAULT_GROUP_ID = "io.github.bluetape4k.javers"
    MODULE_MARKERS = {
      start: "<!-- BOM_PUBLISHED_MODULES:START -->",
      finish: "<!-- BOM_PUBLISHED_MODULES:END -->",
    }.freeze
    MODULE_TOKEN = /`(javers-[a-z0-9-]+)`/
    NON_PUBLISHABLE_PREFIXES = %w[examples- benchmark-].freeze

    def initialize(pom_path:, manual_paths:, group_id: DEFAULT_GROUP_ID)
      @pom_path = File.expand_path(pom_path)
      @manual_paths = manual_paths.transform_keys(&:to_s)
      @group_id = group_id
    end

    def errors
      @errors ||= begin
        pom_modules, pom_errors = read_pom_modules
        manual_modules = {}
        manual_errors = []
        @manual_paths.each do |locale, path|
          manual_modules[locale], errors = read_manual_modules(path, locale)
          manual_errors.concat(errors)
        end

        errors = pom_errors + manual_errors
        non_publishable = pom_modules.select { |artifact| non_publishable?(artifact) }
        unless non_publishable.empty?
          errors << "BOM contains non-publishable modules: #{non_publishable.join(', ')}"
        end

        expected = pom_modules.reject { |artifact| non_publishable?(artifact) }
        manual_modules.each do |locale, modules|
          next if modules.sort == expected.sort

          errors << "#{locale} manual modules differ: expected #{expected.sort.join(', ')}; " \
                    "actual #{modules.sort.join(', ')}"
        end
        locale_sets = manual_modules.values.map(&:sort).uniq
        errors << "locale module sets differ: #{manual_modules.inspect}" unless locale_sets.length <= 1
        errors.sort
      end
    end

    private

    def read_pom_modules
      return [[], ["BOM POM not found: #{@pom_path}"]] unless File.file?(@pom_path)

      document = REXML::Document.new(File.read(@pom_path))
      modules = REXML::XPath.match(
        document,
        "/project/dependencyManagement/dependencies/dependency",
      ).each_with_object([]) do |dependency, result|
        group_id = dependency.elements["groupId"]&.text.to_s.strip
        next unless group_id == @group_id

        artifact = dependency.elements["artifactId"]&.text.to_s.strip
        result << artifact unless artifact.empty?
      end
      errors = ["BOM POM has no #{@group_id} constraints"] if modules.empty?
      duplicates = modules.group_by(&:itself).select { |_artifact, matches| matches.length > 1 }.keys
      errors = Array(errors)
      errors << "BOM POM contains duplicate modules: #{duplicates.sort.join(', ')}" unless duplicates.empty?
      [modules.uniq.sort, errors]
    rescue REXML::ParseException => error
      [[], ["BOM POM is invalid XML: #{error.message.lines.first.to_s.strip}"]]
    end

    def read_manual_modules(path, locale)
      return [[], ["#{locale} manual not found: #{path}"]] unless File.file?(path)

      content = File.read(path)
      pattern = /#{Regexp.escape(MODULE_MARKERS.fetch(:start))}(.*?)#{Regexp.escape(MODULE_MARKERS.fetch(:finish))}/m
      match = content.match(pattern)
      return [[], ["#{locale} manual module markers are missing"]] unless match

      modules = match[1].scan(MODULE_TOKEN).flatten
      return [[], ["#{locale} manual module marker has no modules"]] if modules.empty?

      [modules.uniq.sort, []]
    rescue ArgumentError => error
      [[], ["#{locale} manual is not valid UTF-8: #{error.message}"]]
    end

    def non_publishable?(artifact)
      NON_PUBLISHABLE_PREFIXES.any? { |prefix| artifact.start_with?(prefix) }
    end
  end
end

if $PROGRAM_NAME == __FILE__
  abort("usage: ruby scripts/publication/bom_contract.rb POM EN_MANUAL KO_MANUAL") unless ARGV.length == 3

  contract = Publication::BomContract.new(
    pom_path: ARGV.fetch(0),
    manual_paths: { "en" => ARGV.fetch(1), "ko" => ARGV.fetch(2) },
  )
  errors = contract.errors
  abort(errors.join("\n") + "\nbom-contract: failures=#{errors.length}") unless errors.empty?

  puts "bom-contract: failures=0"
end
