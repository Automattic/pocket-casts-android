# Generate android theme colours from exported CSV from Google Sheet
# To use: ruby generate_themes2.rb themes.csv
require 'csv'
require './download_themes.rb'

filePath = '../../modules/services/compose/src/main/java/au/com/shiftyjelly/pocketcasts/compose/Colors.kt'

class String
  def uncapitalize 
    self[0, 1].downcase + self[1..-1]
  end
end

def int_to_hex(value)
    (2.55 * value.to_f()).round.to_s(16).upcase.rjust(2, '0')
end

tokens = download_themes()
exit if tokens.nil?

themeToCodeLines = {}

tokens.each do |token_attrs|
    key = token_attrs[:key]
    next if key.start_with?("filter_ui_") ||
        key.start_with?("filter_interactive_") ||
        key.start_with?("filter_icon_") ||
        key.start_with?("filter_text_") ||
        key.start_with?("image_") ||
        key.start_with?("podcast_icon_") || 
        key.start_with?("podcast_ui_") || 
        key.start_with?("podcast_text_") ||
        key.start_with?("podcast_interactive_") ||
        key.start_with?("player_background_") ||
        key.start_with?("player_highlight_") ||
        key.start_with?("podcast_on")
        
        
    kotlin_name = token_attrs[:kotlin_name]
    
    token_attrs[:themes].each do |name, attrs|
        next if name.to_s == "classic_dark"
        cleanThemeName = name.to_s.split('_').collect(&:capitalize).join
        lines = themeToCodeLines[cleanThemeName] || []
        hex = attrs[:hex].gsub("#","")
        next if hex.include?("$")
        opacity = attrs[:opacity]
        next if opacity.to_i == 0
        lines << "#{kotlin_name} = Color(0x#{int_to_hex(opacity)}#{hex})"
        themeToCodeLines[cleanThemeName] = lines
    end
end

File.truncate(filePath, 0) if File.exist?(filePath)

File.write(filePath, "// ************ WARNING AUTO GENERATED, DO NOT EDIT ************
package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.ui.graphics.Color
", mode: 'a')

themeToCodeLines.each do |theme, lines|
    File.write(filePath, "
val Theme#{theme}Colors = ThemeColors(
    #{lines.join(",\n    ")}
)
", mode: 'a')
end
