# Generate android theme colours from exported CSV from Google Sheet
# To use: ruby generate_themes2.rb themes.csv
require 'csv'
require './download_themes.rb'

filePathColors = '../../modules/services/ui/src/main/java/au/com/shiftyjelly/pocketcasts/ui/theme/ThemeColor.kt'
filePathStyles = '../../modules/services/ui/src/main/java/au/com/shiftyjelly/pocketcasts/ui/theme/ThemeStyle.kt'

class String
  def uncapitalize 
    self[0, 1].downcase + self[1..-1]
  end
end

def writeThemeValue(hex_val, opacity, tokenName, filePath, themeName)
    if tokenName.start_with?("filterU") || tokenName.start_with?("filterI") || tokenName.start_with?("filterT")
        str = ""
        # deal with special filter overlay colours
        if hex_val == "filter" || hex_val == "$filter" || hex_val == "#filter"
            # the ones without any custom opacity are easy
            if opacity == "100%" || opacity.nil? || opacity.empty?
                str = "    @ColorInt fun #{tokenName}#{themeName}(@ColorInt filterColor: Int): Int {
                              return filterColor
                           }\n\n"
            else
                # tokenize the filter colour to figure out what it should be
                # example string: filter 15% on white
                words = opacity.split

                actual_opacity = words[1].gsub("%", "")
                if words[3] == "white" 
                    originalColor =  "Color.WHITE"
                elsif words[3].start_with?("#")
                    originalColor = "Color.parseColor(\"#{words[3]}\")"
                else 
                    originalColor = "Color.BLACK"
                end
               
                overlayColor = "ColorUtils.colorWithAlpha(filterColor, #{(actual_opacity.to_f / 100.0 * 255.0).round})"

                str = "    @ColorInt fun #{tokenName}#{themeName}(@ColorInt filterColor: Int): Int {
        return ColorUtils.calculateCombinedColor(#{originalColor}, #{overlayColor})
    }\n\n"
            end

        else
            str = "@ColorInt fun #{tokenName}#{themeName}(@ColorInt filterColor: Int): Int { return Color.parseColor(\"#{hex_val}\") }\n\n"
        end

        File.write(filePath, str, mode: 'a')
        return
    elsif tokenName.start_with?("podcast") || tokenName.start_with?("playerBackground") || tokenName.start_with?("playerHighlight")
    	str = ""
    	# deal with special podcast overlay colours
    	if hex_val == "podcast" || hex_val == "$podcast" || hex_val == "#podcast"
            # the ones without any custom opacity are easy
            if opacity == "100%" || opacity.nil? || opacity.empty?
                str = "    @ColorInt fun #{tokenName}#{themeName}(@ColorInt podcastColor: Int): Int {
        return podcastColor
    }\n\n"
            elsif opacity.split.size == 1
            	opacity = opacity.gsub("%", "")
            	str = "    @ColorInt fun #{tokenName}#{themeName}(@ColorInt podcastColor: Int): Int {
        return ColorUtils.colorWithAlpha(podcastColor, #{(opacity.to_f / 100.0 * 255.0).round})
    }\n\n"
            else
                # tokenize the podcast colour to figure out what it should be
                # example string: podcast 15% on #ffffff
                words = opacity.split

                actual_opacity = words[1].gsub("%", "")
                originalColor = "Color.parseColor(\"#{words[3]}\")"
                overlayColor = "ColorUtils.colorWithAlpha(podcastColor, #{(actual_opacity.to_f / 100.0 * 255.0).round})"

                str = "    @ColorInt fun #{tokenName}#{themeName}(@ColorInt podcastColor: Int): Int {
        return ColorUtils.calculateCombinedColor(#{originalColor}, #{overlayColor})
    }\n\n"
            end
        else
            if opacity == "100%" || opacity.nil? || opacity.empty?
                str = "    @ColorInt fun #{tokenName}#{themeName}(@ColorInt podcastColor: Int): Int { return Color.parseColor(\"#{hex_val}\") }\n\n"
            elsif opacity.split.size == 1
                opacity = opacity.gsub("%", "")
                originalColor = "Color.parseColor(\"#{hex_val}\")"
                str = "    @ColorInt fun #{tokenName}#{themeName}(@ColorInt podcastColor: Int): Int {
       return ColorUtils.colorWithAlpha(#{originalColor}, #{(opacity.to_f / 100.0 * 255.0).round})
    }\n\n"
            end
        end

        File.write(filePath, str, mode: 'a')
        return
    end

    if !hex_val.start_with?("#")
        puts "Invalid hex value found #{hex_val}, found in #{tokenName} ignoring"
        return
    end 

	variable_str = "    val #{tokenName}#{themeName} = Color.parseColor(\"#{hex_val}\")"
	if opacity == "100%" || opacity.nil? || opacity.empty?
    	File.write(filePath, "#{variable_str}\n", mode: 'a')
    else
        opacity = opacity.gsub("%", "")
    	File.write(filePath, "#{variable_str}.colorIntWithAlpha(#{(opacity.to_f / 100.0 * 255.0).round})\n", mode: 'a')
    end
end

tokens = download_themes()
exit if tokens.nil?

File.truncate(filePathColors, 0) if File.exist?(filePathColors)
File.truncate(filePathStyles, 0) if File.exist?(filePathStyles)

File.write(filePathColors, "// ************ WARNING AUTO GENERATED, DO NOT EDIT ************
@file:Suppress(\"unused\", \"MemberVisibilityCanBePrivate\", \"UNUSED_PARAMETER\")

package au.com.shiftyjelly.pocketcasts.ui.theme

import android.graphics.Color
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.colorIntWithAlpha

object ThemeColor {\n", mode: 'a')
File.write(filePathStyles, "package au.com.shiftyjelly.pocketcasts.ui.theme

// ************ WARNING AUTO GENERATED, DO NOT EDIT ************\nenum class ThemeStyle {\n", mode: 'a')

index = 0
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

    classic_dark_hex_value = themes[:classic_dark][:hex]
    classic_dark_opacity = themes[:classic_dark][:opacity]

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

    unless token_name == nil || token_name == "Token" || light_hex_value == nil || dark_hex_value == nil
    	token_name = token_name.gsub("$", "").split('-').collect(&:capitalize).join.uncapitalize
        all_token_names << token_name

        File.write(filePathStyles, "    #{token_name},\n", mode: 'a')

    	writeThemeValue(light_hex_value, light_opacity, token_name, filePathColors, "Light")
    	writeThemeValue(dark_hex_value, dark_opacity, token_name, filePathColors, "Dark")
        writeThemeValue(extra_dark_hex_value, extra_dark_opacity, token_name, filePathColors, "ExtraDark")
        writeThemeValue(classic_light_hex_value, classic_light_opacity, token_name, filePathColors, "ClassicLight")
        writeThemeValue(electric_hex_value, electric_opacity, token_name, filePathColors, "Electric")
        writeThemeValue(indigo_hex_value, indigo_opacity, token_name, filePathColors, "Indigo")
        writeThemeValue(radioactive_hex_value, radioactive_opacity, token_name, filePathColors, "Radioactive")
        writeThemeValue(rose_hex_value, rose_opacity, token_name, filePathColors, "Rose")
        writeThemeValue(light_contrast_hex_value, light_contrast_opacity, token_name, filePathColors, "LightContrast")
        writeThemeValue(dark_contrast_hex_value, dark_contrast_opacity, token_name, filePathColors, "DarkContrast")

        index += 1
    end
end

File.write(filePathColors, "\n", mode: 'a')
all_token_names.each_with_index do |token, index|
    if token.start_with?("podcast") || token.start_with?("playerBackground") || token.start_with?("playerHighlight")
        token_str = "    @ColorInt fun #{token}(activeTheme: Theme.ThemeType, @ColorInt podcastColor: Int): Int {
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
        File.write(filePathColors, token_str, mode: 'a')
    elsif token.start_with?("filterU") || token.start_with?("filterI") || token.start_with?("filterT")
        token_str = "    @ColorInt fun #{token}(activeTheme: Theme.ThemeType, @ColorInt filterColor: Int): Int {
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
        File.write(filePathColors, token_str, mode: 'a')
    else
        token_str = "    @ColorInt fun #{token}(theme: Theme.ThemeType): Int {
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
        File.write(filePathColors, token_str, mode: 'a')
    end

    if index != all_token_names.length - 1
        File.write(filePathColors, "\n", mode: 'a')
    end
end

File.write(filePathColors, "}\n", mode: 'a')

File.truncate(filePathStyles, File.size(filePathStyles) - 2) #remove the trailing comma
File.write(filePathStyles, "\n}\n", mode: 'a')