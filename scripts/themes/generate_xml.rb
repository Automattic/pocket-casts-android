require "fileutils"
require './download_themes.rb'

THEME_FILE = "../../modules/services/ui/src/main/res/values/themes.xml"

def write_to_theme_file(output, file_marker)
  contents = IO.read(THEME_FILE)
  new_contents = ""
  reading = true
  contents.lines.each do |line|
    reading = true if line.include?("#{file_marker} - WARNING AUTO GENERATED, DO NOT EDIT - end")
    new_contents << line if reading
    if line.include?("#{file_marker} - WARNING AUTO GENERATED, DO NOT EDIT - begin")
      reading = false 
      new_contents << output
    end
  end
  File.open(THEME_FILE, "w") do |file|
    file.write(new_contents)
  end
end

def write_theme_attrs(tokens)
  output = ""
  tokens.each do |token_attrs|
    next if token_attrs[:user_input]
    key = token_attrs[:key]
    output += %Q[        <attr name="#{key}" format="color" />\n]
  end
  write_to_theme_file(output, "Theme tokens")
end

def write_theme_colors(tokens, theme_name, file_marker)
  output = ""
  tokens.each do |token_attrs|
    key = token_attrs[:key]
    next if token_attrs[:user_input]
    theme = token_attrs[:themes][theme_name.to_sym]
    color = theme[:hex]
    opacity = theme[:opacity]
    alpha = (opacity[0...-1].to_f() / 100.0 * 255.0).to_i().to_s(16)
    alpha = alpha.size == 1 ? "0" + alpha : alpha
    colorWithAlpha = opacity == "100%" ? color : "##{alpha}#{color[1..-1]}"
    output += %Q[        <item name="#{key}">#{colorWithAlpha}</item>\n]
  end
  write_to_theme_file(output, file_marker)
end

tokens = download_themes()
exit if tokens.nil?

write_theme_attrs(tokens)
write_theme_colors(tokens, "light", "Light theme tokens")
write_theme_colors(tokens, "dark", "Dark theme tokens")
write_theme_colors(tokens, "extra_dark", "Extra dark theme tokens")
write_theme_colors(tokens, "classic_light", "Classic light theme tokens")
write_theme_colors(tokens, "electricity", "Electricity theme tokens")
write_theme_colors(tokens, "indigo", "Indigo theme tokens")
write_theme_colors(tokens, "radioactive", "Radioactive theme tokens")
write_theme_colors(tokens, "rose", "Rose theme tokens")
write_theme_colors(tokens, "light_contrast", "Light contrast theme tokens")
write_theme_colors(tokens, "dark_contrast", "Dark contrast theme tokens")

