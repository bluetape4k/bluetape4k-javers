#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "open3"

ROOT = File.expand_path("../..", __dir__)
WIDTH = 1600
HEIGHT = 1040

PALETTE = {
  bg: "#07111f", panel: "#0d1b2d", card: "#11263b", card_alt: "#152d45",
  border: "#31506c", text: "#eef6ff", muted: "#a9bfd2",
  cyan: "#55c7e8", teal: "#5ed6bd", purple: "#b69af5",
  amber: "#e7b85c", rose: "#ef8297", olive: "#9fbd72"
}.freeze

ASSETS = {
  "repository-learning-map" => "overview",
  "audit-snapshot-model" => "architecture",
  "persistence-decision-map" => "persistence",
  "exposed-snapshot-flow" => "persistence",
  "ddd-cqrs-sequence" => "examples"
}.freeze

def esc(text)
  text.to_s.gsub("&", "&amp;").gsub("<", "&lt;").gsub(">", "&gt;")
end

def text(x, y, value, size: 22, fill: PALETTE[:text], anchor: "start", weight: 400, family: "Comic Mono", klass: nil)
  class_attr = klass ? %( class="#{klass}") : ""
  %(<text#{class_attr} x="#{x}" y="#{y}" text-anchor="#{anchor}" font-family="#{family}, monospace" font-size="#{size}" font-weight="#{weight}" fill="#{fill}">#{esc(value)}</text>)
end

def multiline(x, y, lines, size: 22, fill: PALETTE[:text], anchor: "start", gap: 31, weight: 400, family: "Comic Mono")
  lines.each_with_index.map { |line, i| text(x, y + i * gap, line, size: size, fill: fill, anchor: anchor, weight: weight, family: family) }.join("\n")
end

def card(x, y, w, h, title, lines, accent:, subtitle: nil, title_size: 25)
  body_y = subtitle ? y + 88 : y + 76
  <<~SVG
    <g class="card-group" data-card="#{esc(title)}">
      <rect class="card" x="#{x}" y="#{y}" width="#{w}" height="#{h}" rx="22" fill="#{PALETTE[:card]}" stroke="#{accent}" stroke-width="2.5"/>
      #{text(x + 28, y + 43, title, size: title_size, weight: 700, family: "Architects Daughter")}
      #{subtitle ? text(x + 28, y + 70, subtitle, size: 16, fill: PALETTE[:muted]) : ""}
      #{multiline(x + 28, body_y, lines, size: 18, fill: PALETTE[:muted], gap: 27)}
    </g>
  SVG
end

def pill(x, y, label, color, width: nil)
  width ||= [label.length * 11 + 32, 92].max
  <<~SVG
    <g class="label-pill">
      <rect x="#{x}" y="#{y}" width="#{width}" height="34" rx="17" fill="#{PALETTE[:bg]}" stroke="#{color}" stroke-width="2"/>
      #{text(x + width / 2.0, y + 23, label, size: 15, fill: color, anchor: "middle", weight: 700)}
    </g>
  SVG
end

def path(d, color:, marker: true, width: 5, dash: nil, id: nil, klass: "connector", source: nil, target: nil)
  marker_attr = marker ? %( marker-end="url(##{marker_id(color, width >= 6 ? 16 : 14)})") : ""
  dash_attr = dash ? %( stroke-dasharray="#{dash}") : ""
  id_attr = id ? %( id="#{id}") : ""
  data = [source && %(data-source="#{source}"), target && %(data-target="#{target}")].compact.join(" ")
  %(<path#{id_attr} class="#{klass}" #{data} d="#{d}" fill="none" stroke="#{color}" stroke-width="#{width}" stroke-linecap="round" stroke-linejoin="round"#{dash_attr}#{marker_attr}/>)
end

def marker_id(color, size)
  key = PALETTE.key(color) || color.delete("#")
  "arrow-#{key}-#{size}"
end

def marker(color, size)
  id = marker_id(color, size)
  <<~SVG
    <marker id="#{id}" markerWidth="#{size}" markerHeight="#{size}" refX="9" refY="5" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 10 10">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#{color}" stroke="#{color}" stroke-width="0.6"/>
    </marker>
  SVG
end

def defs
  colors = %i[cyan teal purple amber rose olive].map { |name| PALETTE[name] }
  <<~SVG
    <defs>
      <filter id="shadow" x="-20%" y="-20%" width="140%" height="150%">
        <feDropShadow dx="0" dy="8" stdDeviation="10" flood-color="#000814" flood-opacity="0.42"/>
      </filter>
      #{colors.flat_map { |color| [marker(color, 14), marker(color, 16)] }.join("\n")}
    </defs>
  SVG
end

def shell(title, subtitle, body, legend: nil)
  <<~SVG
    <?xml version="1.0" encoding="UTF-8"?>
    <svg xmlns="http://www.w3.org/2000/svg" width="#{WIDTH}" height="#{HEIGHT}" viewBox="0 0 #{WIDTH} #{HEIGHT}" role="img" aria-labelledby="title desc">
      <title id="title">#{esc(title)}</title>
      <desc id="desc">#{esc(subtitle)}</desc>
      #{defs}
      <rect width="#{WIDTH}" height="#{HEIGHT}" fill="#{PALETTE[:bg]}"/>
      <rect class="frame" x="36" y="32" width="1528" height="976" rx="32" fill="none" stroke="#{PALETTE[:border]}" stroke-width="2"/>
      #{text(76, 92, title, size: 42, weight: 700, family: "Architects Daughter", klass: "title")}
      #{text(78, 128, subtitle, size: 18, fill: PALETTE[:muted], klass: "subtitle")}
      #{body}
      #{legend}
      #{text(1520, 980, "bluetape4k-javers 0.3.0", size: 15, fill: "#7892aa", anchor: "end")}
    </svg>
  SVG
end

def repository_learning_map
  body = []
  body << card(90, 210, 260, 176, "1 · Audit model", ["commit + snapshot", "change + shadow"], accent: PALETTE[:cyan])
  body << card(455, 210, 290, 176, "2 · Repository SPI", ["CdoSnapshotRepository", "codec + JQL filters"], accent: PALETTE[:teal])
  body << card(850, 176, 280, 150, "3A · Exposed", ["durable SQL history", "restart recovery"], accent: PALETTE[:purple])
  body << card(850, 350, 280, 150, "3B · Redis", ["snapshot store", "durability is yours"], accent: PALETTE[:amber])
  body << card(850, 524, 280, 150, "3C · Kafka", ["publication stream", "no repository reads"], accent: PALETTE[:rose])
  body << card(1235, 210, 270, 176, "4 · DDD path", ["persist then audit", "publish event"], accent: PALETTE[:cyan])
  body << card(455, 650, 290, 176, "5 · Example", ["order command", "Kafka to Redis view"], accent: PALETTE[:olive])
  body << card(90, 650, 260, 176, "6 · Operations", ["failure windows", "metrics + recovery"], accent: PALETTE[:rose])
  body << path("M 350 298 H 455", color: PALETTE[:cyan], source: "audit", target: "spi")
  body << path("M 745 286 H 802 Q 816 286 816 272 V 265 Q 816 251 830 251 H 850", color: PALETTE[:purple], source: "spi", target: "exposed")
  body << path("M 745 298 H 790 Q 804 298 804 312 V 411 Q 804 425 818 425 H 850", color: PALETTE[:amber], source: "spi", target: "redis")
  body << path("M 745 310 H 778 Q 792 310 792 324 V 585 Q 792 599 806 599 H 850", color: PALETTE[:rose], source: "spi", target: "kafka")
  body << path("M 1130 251 H 1235", color: PALETTE[:purple], source: "exposed", target: "ddd")
  body << path("M 1130 425 H 1175 Q 1189 425 1189 411 V 312 Q 1189 298 1203 298 H 1235", color: PALETTE[:amber], source: "redis", target: "ddd")
  body << path("M 1130 599 H 1185 Q 1199 599 1199 585 V 360 Q 1199 346 1213 346 H 1235", color: PALETTE[:rose], source: "kafka", target: "ddd")
  body << path("M 1505 330 H 1528 Q 1542 330 1542 344 V 850 Q 1542 864 1528 864 H 714 Q 700 864 700 850 V 826", color: PALETTE[:cyan], source: "ddd", target: "example")
  body << path("M 455 738 H 350", color: PALETTE[:olive], source: "example", target: "operations")
  legend = <<~SVG
    <g class="legend">#{pill(90, 884, "Concept", PALETTE[:cyan], width: 126)}#{pill(230, 884, "Durable", PALETTE[:purple], width: 126)}#{pill(370, 884, "Cache", PALETTE[:amber], width: 112)}#{pill(496, 884, "Stream", PALETTE[:rose], width: 120)}</g>
  SVG
  shell("Repository learning map", "Read by responsibility; choose persistence by recovery need.", body.join("\n"), legend: legend)
end

def audit_snapshot_model
  body = []
  body << card(92, 238, 280, 190, "Domain object", ["current application state", "owned by the app store"], accent: PALETTE[:cyan])
  body << card(470, 238, 300, 190, "JaVers commit", ["author + time + properties", "one audited operation"], accent: PALETTE[:teal])
  body << card(900, 180, 290, 190, "Commit metadata", ["CommitId + sequence", "author + instant"], accent: PALETTE[:purple])
  body << card(900, 470, 290, 218, "CdoSnapshot", ["GlobalId + version", "state + changed fields", "type + commit metadata"], accent: PALETTE[:amber])
  body << card(1280, 220, 240, 166, "Changes", ["what differs?", "computed by JaVers"], accent: PALETTE[:rose])
  body << card(1280, 510, 240, 166, "Shadows", ["what did it look like?", "reconstructed history"], accent: PALETTE[:olive])
  body << card(470, 650, 300, 190, "Repository SPI", ["encode / decode", "query + head contract"], accent: PALETTE[:cyan])
  body << path("M 372 333 H 470", color: PALETTE[:cyan], source: "object", target: "commit")
  body << path("M 770 298 H 840 Q 850 298 850 288 V 285 Q 850 275 860 275 H 900", color: PALETTE[:purple], source: "commit", target: "metadata")
  body << path("M 770 356 H 832 Q 846 356 846 370 V 565 Q 846 579 860 579 H 900", color: PALETTE[:amber], source: "commit", target: "snapshot")
  body << path("M 1045 370 V 470", color: PALETTE[:purple], marker: false, width: 4, dash: "10 9", source: "metadata", target: "snapshot")
  body << path("M 1190 560 H 1232 Q 1246 560 1246 546 V 317 Q 1246 303 1260 303 H 1280", color: PALETTE[:rose], source: "snapshot", target: "changes")
  body << path("M 1190 610 H 1280", color: PALETTE[:olive], source: "snapshot", target: "shadows")
  body << path("M 900 635 H 818 Q 804 635 804 649 V 731 Q 804 745 790 745 H 770", color: PALETTE[:amber], source: "snapshot", target: "spi")
  body << path("M 620 650 V 428", color: PALETTE[:teal], source: "spi", target: "commit")
  body << pill(795, 229, "creates 1", PALETTE[:purple], width: 116)
  body << pill(782, 482, "creates 1..n", PALETTE[:amber], width: 144)
  body << pill(929, 399, "belongs to", PALETTE[:purple], width: 132)
  legend = %(<g class="legend">#{pill(96, 893, "Solid = creation / query", PALETTE[:cyan], width: 248)}#{pill(360, 893, "Dashed = metadata association", PALETTE[:purple], width: 278)}</g>)
  shell("Audit snapshot model", "A commit records an operation; snapshots preserve state for change and shadow queries.", body.join("\n"), legend: legend)
end

def persistence_decision_map
  body = []
  body << card(90, 250, 300, 208, "Recovery question", ["What must remain after", "process or store failure?"], accent: PALETTE[:cyan])
  body << card(510, 178, 300, 166, "Durable audit history", ["SQL operations", "restart head recovery"], accent: PALETTE[:purple])
  body << card(510, 430, 300, 166, "Redis is accepted store", ["fast snapshot access", "backup / persistence owned"], accent: PALETTE[:amber], title_size: 22)
  body << card(510, 682, 300, 166, "Publish snapshots", ["consumer-owned queries", "retention + replay policy"], accent: PALETTE[:rose])
  body << card(960, 178, 300, 166, "Choose Exposed", ["commit + snapshot tables", "not app business rows"], accent: PALETTE[:purple])
  body << card(960, 430, 300, 166, "Choose Redis", ["Lettuce or Redisson", "not a near-cache"], accent: PALETTE[:amber])
  body << card(960, 682, 300, 166, "Choose Kafka", ["write-only repository", "no JQL reads"], accent: PALETTE[:rose])
  body << card(1325, 380, 200, 250, "Always add", ["retry policy", "stable IDs", "reconciliation", "observability"], accent: PALETTE[:teal], title_size: 24)
  body << path("M 390 320 H 450 Q 464 320 464 306 V 275 Q 464 261 478 261 H 510", color: PALETTE[:purple], source: "question", target: "durable")
  body << path("M 390 354 H 510", color: PALETTE[:amber], source: "question", target: "redis-need")
  body << path("M 390 390 H 450 Q 464 390 464 404 V 751 Q 464 765 478 765 H 510", color: PALETTE[:rose], source: "question", target: "publish")
  body << path("M 810 261 H 960", color: PALETTE[:purple], source: "durable", target: "exposed")
  body << path("M 810 513 H 960", color: PALETTE[:amber], source: "redis-need", target: "redis")
  body << path("M 810 765 H 960", color: PALETTE[:rose], source: "publish", target: "kafka")
  body << path("M 1260 261 H 1285 Q 1299 261 1299 275 V 411 Q 1299 425 1313 425 H 1325", color: PALETTE[:purple], source: "exposed", target: "always")
  body << path("M 1260 513 H 1325", color: PALETTE[:amber], source: "redis", target: "always")
  body << path("M 1260 765 H 1285 Q 1299 765 1299 751 V 599 Q 1299 585 1313 585 H 1325", color: PALETTE[:rose], source: "kafka", target: "always")
  body << pill(398, 278, "restart", PALETTE[:purple], width: 102)
  body << pill(398, 474, "Redis", PALETTE[:amber], width: 102)
  body << pill(398, 730, "fan out", PALETTE[:rose], width: 102)
  legend = %(<g class="legend">#{pill(90, 904, "Purple - SQL durability", PALETTE[:purple], width: 224)}#{pill(330, 904, "Amber - Redis store", PALETTE[:amber], width: 202)}#{pill(548, 904, "Rose - publication", PALETTE[:rose], width: 192)}</g>)
  shell("Persistence decision map", "Select one primary responsibility; composition and recovery remain application concerns.", body.join("\n"), legend: legend)
end

def exposed_snapshot_flow
  body = []
  body << card(78, 236, 250, 176, "JaVers persist", ["commit.snapshots", "serialized by lock"], accent: PALETTE[:cyan])
  body << card(420, 236, 280, 176, "saveSnapshot", ["encode JSON", "one snapshot at a time"], accent: PALETTE[:teal])
  body << card(820, 160, 300, 176, "CommitTable", ["insert if absent", "sequence starts at 0"], accent: PALETTE[:purple])
  body << card(820, 420, 300, 176, "CdoSnapshotTable", ["GlobalId + commitId", "version + state"], accent: PALETTE[:amber])
  body << card(1250, 236, 270, 176, "updateCommitId", ["separate transaction", "stores Snowflake seq"], accent: PALETTE[:rose])
  body << card(420, 670, 280, 176, "loadHeadId", ["highest sequence", "restores restart head"], accent: PALETTE[:purple])
  body << card(820, 670, 300, 176, "loadSnapshots", ["filter GlobalId", "version DESC"], accent: PALETTE[:olive])
  body << path("M 328 324 H 420", color: PALETTE[:cyan], source: "persist", target: "save")
  body << path("M 700 292 H 770 Q 784 292 784 278 V 262 Q 784 248 798 248 H 820", color: PALETTE[:purple], source: "save", target: "commit-table")
  body << path("M 700 356 H 770 Q 784 356 784 370 V 494 Q 784 508 798 508 H 820", color: PALETTE[:amber], source: "save", target: "snapshot-table")
  body << path("M 1250 304 H 1120", color: PALETTE[:purple], source: "update", target: "commit-table")
  body << path("M 1120 508 H 1180 Q 1194 508 1194 494 V 370 Q 1194 356 1208 356 H 1250", color: PALETTE[:amber], source: "snapshot-table", target: "update")
  body << path("M 1385 412 V 612 Q 1385 636 1361 636 H 674 Q 650 636 650 660 V 670", color: PALETTE[:rose], source: "update", target: "head")
  body << path("M 820 758 H 700", color: PALETTE[:olive], source: "load", target: "head")
  body << path("M 970 596 V 670", color: PALETTE[:amber], source: "snapshot-table", target: "load")
  body << pill(726, 203, "tx per snapshot", PALETTE[:purple], width: 152)
  body << pill(1135, 438, "after snapshots", PALETTE[:rose], width: 152)
  legend = %(<g class="legend">#{pill(78, 904, "Important: this is not one atomic database transaction", PALETTE[:rose], width: 510)}</g>)
  shell("Exposed snapshot flow", "Each snapshot write and the final sequence update use separate Exposed transactions.", body.join("\n"), legend: legend)
end

def participant(x, title, subtitle, color)
  <<~SVG
    <g class="participant-group" data-participant="#{esc(title)}">
      <rect class="header" x="#{x - 82}" y="168" width="164" height="86" rx="18" fill="#{PALETTE[:card]}" stroke="#{color}" stroke-width="2.5"/>
      #{text(x, 202, title, size: 19, fill: PALETTE[:text], anchor: "middle", weight: 700, family: "Architects Daughter", klass: "participant")}
      #{text(x, 229, subtitle, size: 13, fill: PALETTE[:muted], anchor: "middle", klass: "role")}
      <line class="lifeline" x1="#{x}" y1="254" x2="#{x}" y2="920" stroke="#{PALETTE[:border]}" stroke-width="3" stroke-dasharray="8 10"/>
    </g>
  SVG
end

def activation(x, y, h, color)
  %(<rect class="activation" x="#{x - 7}" y="#{y}" width="14" height="#{h}" rx="5" fill="#{PALETTE[:panel]}" stroke="#{color}" stroke-width="2"/>)
end

def message(x1, x2, y, number, label, color, reverse: false)
  sx, tx = reverse ? [x2, x1] : [x1, x2]
  width = [label.length * 9 + 74, 164].max
  px = (x1 + x2 - width) / 2.0
  <<~SVG
    <g class="message" data-order="#{number}">
      #{path("M #{sx} #{y} H #{tx}", color: color, width: 6, source: "p#{sx}", target: "p#{tx}", klass: "connector seq")}
      <rect class="pill" x="#{px}" y="#{y - 34}" width="#{width}" height="28" rx="14" fill="#{PALETTE[:bg]}" stroke="#{color}" stroke-width="2"/>
      <circle cx="#{px + 17}" cy="#{y - 20}" r="12" fill="#{PALETTE[:bg]}" stroke="#{color}" stroke-width="2"/>
      #{text(px + 17, y - 15, number, size: 13, fill: color, anchor: "middle", weight: 700, klass: "num")}
      #{text(px + 36, y - 15, label, size: 14, fill: color, weight: 700, klass: "labelText")}
    </g>
  SVG
end

def ddd_cqrs_sequence
  xs = [125, 350, 575, 800, 1025, 1250, 1475]
  body = []
  [["Client", "command", :cyan], ["Handler", "application", :cyan], ["Order repo", "source state", :purple], ["JaVers", "audit history", :teal], ["Kafka", "event stream", :amber], ["Consumer", "projection", :rose], ["Redis", "query view", :olive]].each_with_index do |(title, subtitle, color), i|
    body << participant(xs[i], title, subtitle, PALETTE[color])
  end
  body << activation(xs[1], 308, 390, PALETTE[:cyan])
  body << activation(xs[2], 382, 216, PALETTE[:purple])
  body << activation(xs[3], 474, 90, PALETTE[:teal])
  body << activation(xs[5], 730, 126, PALETTE[:rose])
  body << message(xs[0], xs[1], 330, 1, "handle(command)", PALETTE[:cyan])
  body << message(xs[1], xs[2], 410, 2, "persist aggregate", PALETTE[:purple])
  body << message(xs[2], xs[1], 466, 3, "saved order", PALETTE[:teal], reverse: true)
  body << message(xs[1], xs[3], 520, 4, "javers.commit", PALETTE[:teal])
  body << message(xs[1], xs[4], 620, 5, "publishAll(events)", PALETTE[:amber])
  body << message(xs[1], xs[0], 676, 6, "return order", PALETTE[:cyan], reverse: true)
  body << <<~SVG
    <g class="chronological-frame" data-frame="projection-delivery">
      <rect class="alt" x="920" y="690" width="640" height="230" rx="20" fill="none" stroke="#{PALETTE[:rose]}" stroke-width="2.5" stroke-dasharray="10 8"/>
      <rect x="920" y="690" width="164" height="32" rx="12" fill="#{PALETTE[:bg]}" stroke="#{PALETTE[:rose]}" stroke-width="2"/>
      #{text(1002, 712, "async projection", size: 15, fill: PALETTE[:rose], anchor: "middle", weight: 700)}
      <line x1="920" y1="812" x2="1560" y2="812" stroke="#{PALETTE[:border]}" stroke-width="2" stroke-dasharray="8 8"/>
      #{text(940, 895, "failure: retry / reconcile; delivery is not atomic", size: 14, fill: PALETTE[:rose], weight: 700)}
    </g>
  SVG
  body << message(xs[4], xs[5], 758, 7, "poll domain event", PALETTE[:rose])
  body << message(xs[5], xs[6], 866, 8, "upsert OrderSummary", PALETTE[:olive])
  shell("DDD + CQRS order sequence", "Source state, audit history, event delivery, and Redis projection are four separate boundaries.", body.join("\n"))
end

def svg_for(name)
  case name
  when "repository-learning-map" then repository_learning_map
  when "audit-snapshot-model" then audit_snapshot_model
  when "persistence-decision-map" then persistence_decision_map
  when "exposed-snapshot-flow" then exposed_snapshot_flow
  when "ddd-cqrs-sequence" then ddd_cqrs_sequence
  else raise ArgumentError, "unknown asset: #{name}"
  end
end

targets = ARGV.empty? ? ASSETS.keys : ARGV
unknown = targets - ASSETS.keys
abort "Unknown assets: #{unknown.join(', ')}" unless unknown.empty?

targets.each do |name|
  dir = File.join(ROOT, "docs/manual/assets", ASSETS.fetch(name))
  FileUtils.mkdir_p(dir)
  svg = File.join(dir, "#{name}.svg")
  png = File.join(dir, "#{name}.png")
  File.write(svg, svg_for(name).gsub(/[ \t]+\n/, "\n"))
  system("xmllint", "--noout", svg) || abort("xmllint failed: #{svg}")
  out, status = Open3.capture2e("cairosvg", svg, "-o", png, "-s", "2")
  abort("cairosvg failed: #{out}") unless status.success?
  puts "rendered #{name}: #{svg} -> #{png}"
end
