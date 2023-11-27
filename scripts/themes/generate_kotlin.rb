# frozen_string_literal: true

# Generate android theme colours from exported CSV from Google Sheet
# To use: ruby generate_themes2.rb themes.csv
require 'csv'
require './download_themes'

FILE_PATH_COLORS = '../../modules/services/ui/src/main/java/au/com/shiftyjelly/pocketcasts/ui/theme/ThemeColor.kt'
FILE_PATH_STYLES = '../../modules/services/ui/src/main/java/au/com/shiftyjelly/pocketcasts/ui/theme/ThemeStyle.kt'

class String
  def uncapitalize
    self[0, 1].downcase + self[1..]
  end
end

def write_theme_value(hex_val, opacity, token_name, file_path, theme_name)
  if token_name.start_with?('filterU') || token_name.start_with?('filterI') || token_name.start_with?('filterT')
    str = ''
    # deal with special filter overlay colours
    if ['filter', '$filter', '#filter'].include?(hex_val)
      # the ones without any custom opacity are easy
      if opacity == '100%' || opacity.nil? || opacity.empty?
        str = "    @ColorInt fun #{token_name}#{theme_name}(@ColorInt filterColor: Int): Int {
                              return filterColor
                           }\n\n"
      else
        # tokenize the filter colour to figure out what it should be
        # example string: filter 15% on white
        words = opacity.split

        actual_opacity = words[1].gsub('%', '')
        original_color = if words[3] == 'white'
                           'Color.WHITE'
                         elsif words[3].start_with?('#')
                           "Color.parseColor(\"#{words[3]}\")"
                         else
                           'Color.BLACK'
                         end

        overlay_color = "ColorUtils.colorWithAlpha(filterColor, #{(actual_opacity.to_f / 100.0 * 255.0).round})"

        str = "    @ColorInt fun #{token_name}#{theme_name}(@ColorInt filterColor: Int): Int {
        return ColorUtils.calculateCombinedColor(#{original_color}, #{overlay_color})
    }\n\n"
      end

    else
      str = "@ColorInt fun #{token_name}#{theme_name}(@ColorInt filterColor: Int): Int { return Color.parseColor(\"#{hex_val}\") }\n\n"
    end

    File.write(file_path, str, mode: 'a')
    return
  elsif token_name.start_with?('podcast') || token_name.start_with?('playerBackground') || token_name.start_with?('playerHighlight')
    str = ''
    # deal with special podcast overlay colours
    if ['podcast', '$podcast', '#podcast'].include?(hex_val)
      # the ones without any custom opacity are easy
      if opacity == '100%' || opacity.nil? || opacity.empty?
        str = "    @ColorInt fun #{token_name}#{theme_name}(@ColorInt podcastColor: Int): Int {
        return podcastColor
    }\n\n"
      elsif opacity.split.size == 1
        opacity = opacity.gsub('%', '')
        str = "    @ColorInt fun #{token_name}#{theme_name}(@ColorInt podcastColor: Int): Int {
        return ColorUtils.colorWithAlpha(podcastColor, #{(opacity.to_f / 100.0 * 255.0).round})
    }\n\n"
      else
        # tokenize the podcast colour to figure out what it should be
        # example string: podcast 15% on #ffffff
        words = opacity.split

        actual_opacity = words[1].gsub('%', '')
        original_color = "Color.parseColor(\"#{words[3]}\")"
        overlay_color = "ColorUtils.colorWithAlpha(podcastColor, #{(actual_opacity.to_f / 100.0 * 255.0).round})"

        str = "    @ColorInt fun #{token_name}#{theme_name}(@ColorInt podcastColor: Int): Int {
        return ColorUtils.calculateCombinedColor(#{original_color}, #{overlay_color})
    }\n\n"
      end
    elsif opacity == '100%' || opacity.nil? || opacity.empty?
      str = "    @ColorInt fun #{token_name}#{theme_name}(@ColorInt podcastColor: Int): Int { return Color.parseColor(\"#{hex_val}\") }\n\n"
    elsif opacity.split.size == 1
      opacity = opacity.gsub('%', '')
      original_color = "Color.parseColor(\"#{hex_val}\")"
      str = "    @ColorInt fun #{token_name}#{theme_name}(@ColorInt podcastColor: Int): Int {
       return ColorUtils.colorWithAlpha(#{original_color}, #{(opacity.to_f / 100.0 * 255.0).round})
    }\n\n"
    end

    File.write(file_path, str, mode: 'a')
    return
  end

  unless hex_val.start_with?('#')
    puts "Invalid hex value found #{hex_val}, found in #{token_name} ignoring"
    return
  end

  variable_str = "    val #{token_name}#{theme_name} = Color.parseColor(\"#{hex_val}\")"
  if opacity == '100%' || opacity.nil? || opacity.empty?
    File.write(file_path, "#{variable_str}\n", mode: 'a')
  else
    opacity = opacity.gsub('%', '')
    File.write(file_path, "#{variable_str}.colorIntWithAlpha(#{(opacity.to_f / 100.0 * 255.0).round})\n", mode: 'a')
  end
end

tokens = download_themes
exit if tokens.nil?

File.truncate(FILE_PATH_COLORS, 0) if File.exist?(FILE_PATH_COLORS)
File.truncate(FILE_PATH_STYLES, 0) if File.exist?(FILE_PATH_STYLES)

File.write(FILE_PATH_COLORS, "// ************ WARNING AUTO GENERATED, DO NOT EDIT ************
@file:Suppress(\"unused\", \"MemberVisibilityCanBePrivate\", \"UNUSED_PARAMETER\")

package au.com.shiftyjelly.pocketcasts.ui.theme

import android.graphics.Color
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.colorIntWithAlpha

object ThemeColor {\n", mode: 'a')
File.write(FILE_PATH_STYLES, "package au.com.shiftyjelly.pocketcasts.ui.theme

// ************ WARNING AUTO GENERATED, DO NOT EDIT ************\nenum class ThemeStyle {\n", mode: 'a')

all_token_names = []

tokens.each do |token_attrs|
  token_name = token_attrs[:token_name]

  themes = token_attrs[:themes]

  light_hex_value = themes[:light][:hex]
  light_opacity = themes[:light][:opacity]

  dark_hex_value = themes[:dark][:hex]
  dark_opacity = themes[:dark][:opacity]

  extra_dark_hex_value = themes[:extra_dark][:hex]
  extra_dark_opacity = themes[:extra_dark][:opacity]

  classic_light_hex_value = themes[:classic_light][:hex]
  classic_light_opacity = themes[:classic_light][:opacity]

  # unused
  # classic_dark_hex_value = themes[:classic_dark][:hex]
  # classic_dark_opacity = themes[:classic_dark][:opacity]

  electric_hex_value = themes[:electricity][:hex]
  electric_opacity = themes[:electricity][:opacity]

  indigo_hex_value = themes[:indigo][:hex]
  indigo_opacity = themes[:indigo][:opacity]

  radioactive_hex_value = themes[:radioactive][:hex]
  radioactive_opacity = themes[:radioactive][:opacity]

  rose_hex_value = themes[:rose][:hex]
  rose_opacity = themes[:rose][:opacity]

  light_contrast_hex_value = themes[:light_contrast][:hex]
  light_contrast_opacity = themes[:light_contrast][:opacity]

  dark_contrast_hex_value = themes[:dark_contrast][:hex]
  dark_contrast_opacity = themes[:dark_contrast][:opacity]

  next if token_name.nil? || token_name == 'Token' || light_hex_value.nil? || dark_hex_value.nil?

  token_name = token_name.gsub('$', '').split('-').collect(&:capitalize).join.uncapitalize
  all_token_names << token_name

  File.write(FILE_PATH_STYLES, "    #{token_name},\n", mode: 'a')

  write_theme_value(light_hex_value, light_opacity, token_name, FILE_PATH_COLORS, 'Light')
  write_theme_value(dark_hex_value, dark_opacity, token_name, FILE_PATH_COLORS, 'Dark')
  write_theme_value(extra_dark_hex_value, extra_dark_opacity, token_name, FILE_PATH_COLORS, 'ExtraDark')
  write_theme_value(classic_light_hex_value, classic_light_opacity, token_name, FILE_PATH_COLORS, 'ClassicLight')
  write_theme_value(electric_hex_value, electric_opacity, token_name, FILE_PATH_COLORS, 'Electric')
  write_theme_value(indigo_hex_value, indigo_opacity, token_name, FILE_PATH_COLORS, 'Indigo')
  write_theme_value(radioactive_hex_value, radioactive_opacity, token_name, FILE_PATH_COLORS, 'Radioactive')
  write_theme_value(rose_hex_value, rose_opacity, token_name, FILE_PATH_COLORS, 'Rose')
  write_theme_value(light_contrast_hex_value, light_contrast_opacity, token_name, FILE_PATH_COLORS, 'LightContrast')
  write_theme_value(dark_contrast_hex_value, dark_contrast_opacity, token_name, FILE_PATH_COLORS, 'DarkContrast')
end

File.write(FILE_PATH_COLORS, "\n", mode: 'a')
all_token_names.each_with_index do |token, index|
  token_str = if token.start_with?('podcast') || token.start_with?('playerBackground') || token.start_with?('playerHighlight')
                "    @ColorInt fun #{token}(activeTheme: Theme.ThemeType, @ColorInt podcastColor: Int): Int {
        return when (activeTheme) {
            Theme.ThemeType.LIGHT ->
                #{token}Light(podcastColor)
            Theme.ThemeType.DARK ->
                #{token}Dark(podcastColor)
            Theme.ThemeType.EXTRA_DARK ->
                #{token}ExtraDark(podcastColor)
            Theme.ThemeType.ELECTRIC ->
                #{token}Electric(podcastColor)
            Theme.ThemeType.CLASSIC_LIGHT ->
                #{token}ClassicLight(podcastColor)
            Theme.ThemeType.INDIGO ->
                #{token}Indigo(podcastColor)
            Theme.ThemeType.RADIOACTIVE ->
                #{token}Radioactive(podcastColor)
            Theme.ThemeType.ROSE ->
                #{token}Rose(podcastColor)
            Theme.ThemeType.LIGHT_CONTRAST ->
                #{token}LightContrast(podcastColor)
            Theme.ThemeType.DARK_CONTRAST ->
                #{token}DarkContrast(podcastColor)
        }
    }\n"
              elsif token.start_with?('filterU') || token.start_with?('filterI') || token.start_with?('filterT')
                "    @ColorInt fun #{token}(activeTheme: Theme.ThemeType, @ColorInt filterColor: Int): Int {
        return when (activeTheme) {
            Theme.ThemeType.LIGHT ->
                #{token}Light(filterColor)
            Theme.ThemeType.DARK ->
                #{token}Dark(filterColor)
            Theme.ThemeType.EXTRA_DARK ->
                #{token}ExtraDark(filterColor)
            Theme.ThemeType.ELECTRIC ->
                #{token}Electric(filterColor)
            Theme.ThemeType.CLASSIC_LIGHT ->
                #{token}ClassicLight(filterColor)
            Theme.ThemeType.INDIGO ->
                #{token}Indigo(filterColor)
            Theme.ThemeType.RADIOACTIVE ->
                #{token}Radioactive(filterColor)
            Theme.ThemeType.ROSE ->
                #{token}Rose(filterColor)
            Theme.ThemeType.LIGHT_CONTRAST ->
                #{token}LightContrast(filterColor)
            Theme.ThemeType.DARK_CONTRAST ->
                #{token}DarkContrast(filterColor)
            }
        }\n"
              else
                "    @ColorInt fun #{token}(theme: Theme.ThemeType): Int {
        return when (theme) {
            Theme.ThemeType.LIGHT ->
                #{token}Light
            Theme.ThemeType.DARK ->
                #{token}Dark
            Theme.ThemeType.EXTRA_DARK ->
                #{token}ExtraDark
            Theme.ThemeType.ELECTRIC ->
                #{token}Electric
            Theme.ThemeType.CLASSIC_LIGHT ->
                #{token}ClassicLight
            Theme.ThemeType.INDIGO ->
                #{token}Indigo
            Theme.ThemeType.RADIOACTIVE ->
                #{token}Radioactive
            Theme.ThemeType.ROSE ->
                #{token}Rose
            Theme.ThemeType.LIGHT_CONTRAST ->
                #{token}LightContrast
            Theme.ThemeType.DARK_CONTRAST ->
                #{token}DarkContrast
        }
    }\n"
              end
  File.write(FILE_PATH_COLORS, token_str, mode: 'a')

  File.write(FILE_PATH_COLORS, "\n", mode: 'a') if index != all_token_names.length - 1
end

File.write(FILE_PATH_COLORS, "}\n", mode: 'a')

File.truncate(FILE_PATH_STYLES, File.size(FILE_PATH_STYLES) - 2) # remove the trailing comma
File.write(FILE_PATH_STYLES, "\n}\n", mode: 'a')
