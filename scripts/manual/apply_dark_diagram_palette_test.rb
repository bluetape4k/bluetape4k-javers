#!/usr/bin/env ruby
# frozen_string_literal: true

require "minitest/autorun"
require_relative "apply_dark_diagram_palette"

class ApplyDarkDiagramPaletteTest < Minitest::Test
  def test_converts_light_surfaces_text_accents_and_geometry_metadata
    source = <<~SVG
      <svg xmlns="http://www.w3.org/2000/svg">
        <rect fill="#f7fafc"/>
        <rect fill="#ffffff" stroke="#cbd5e1"/>
        <rect fill="#dbeafe" stroke="#2563eb" filter="url(#softShadow)"/>
        <rect fill="#eff6ff" stroke="#3b82f6"/>
        <rect fill="#ecfdf5" stroke="#10b981"/>
        <text fill="#183247">Core</text>
        <text fill="#59677a">Supporting detail</text>
        <style>.label{fill:#172033}.section{fill:#4b5563}</style>
        <path d="M 0 0 H 10" stroke="#2563eb" stroke-width="2.1" marker-end="url(#arrowBlue)"/>
        <path d="M 0 0 H 10" stroke="#f59e0b" stroke-width="2.2"/>
        <marker markerWidth="12" markerHeight="12"/>
      </svg>
    SVG

    transformed = DarkDiagramPalette.transform(source)

    assert_includes transformed, 'fill="#020617"'
    assert_includes transformed, 'fill="#172033" stroke="#536377"'
    assert_includes transformed, '<rect class="card" fill="#223047" stroke="#5b8def"'
    assert_includes transformed, '<text fill="#f8fafc">Core</text>'
    assert_includes transformed, '<text fill="#d5dfeb">Supporting detail</text>'
    assert_includes transformed, '.label{fill:#f8fafc}.section{fill:#d5dfeb}'
    assert_includes transformed, 'fill="#223047" stroke="#5b8def"'
    assert_includes transformed, 'fill="#223047" stroke="#4fb8a8"'
    assert_includes transformed, 'stroke="#d0a24c" stroke-width="4"'
    assert_includes transformed, '<path class="connector"'
    assert_includes transformed, 'stroke-width="4"'
    assert_includes transformed, 'markerWidth="14" markerHeight="14"'
  end

  def test_rejects_non_svg_content
    error = assert_raises(ArgumentError) { DarkDiagramPalette.transform("not svg") }

    assert_equal "expected an SVG document", error.message
  end
end
