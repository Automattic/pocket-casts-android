#!/usr/bin/env ruby
require 'rubygems'
require 'fileutils'
require 'net/http'
require 'uri'
require 'nokogiri'
require 'typhoeus'

Typhoeus::Config.user_agent = "custom user agent"

languages = [
    { label: "Chinese (China)", code: "zh-cn", folder: "values-zh" },
    { label: "Chinese (Taiwan)", code: "zh-tw", folder: "values-zh-rTW" },
    { label: "German", code: "de", folder: "values-de" },
    { label: "Italian", code: "it", folder: "values-it" },
    { label: "Japanese", code: "ja", folder: "values-ja" },
    { label: "Portuguese (Brazil)", code: "pt-br", folder: "values-pt-rBR" },
    { label: "Spanish (Spain)", code: "es", folder: "values-es" },
    { label: "Spanish (Mexico)", code: "es-mx", folder: "values-es-rMX" },
    { label: "Swedish", code: "sv", folder: "values-sv" },
    { label: "Dutch", code: "nl", folder: "values-nl" },
    { label: "French (France)", code: "fr", folder: "values-fr" },
    { label: "French (Canada)", code: "fr-ca", folder: "values-fr-rCA" },
    { label: "Russian", code: "ru", folder: "values-ru" },
    { label: "Arabic", code: "ar", folder: "values-ar" },
    { label: "English (UK)", code: "en-gb", folder: "values-en-rGB" },
    { label: "Norwegian", code: "nb", folder: "values-nb" }
]

def write_string_to_file(contents, file_name)
    File.open(file_name, "w") do |file|
        file.write(contents)
    end
end

def clean_xml(xml)
    doc = Nokogiri.XML(xml)
    doc.xpath('/resources/string').each do |string|
        string.remove if string.text.length == 0 || string.text.include?("@string")
        # remove nbsp characters
        string.content = string.content.gsub(/[[:space:]]/, ' ') if string.text.length > 0
    end
    xml = doc.to_xml
    # remove empty lines
    xml = xml.each_line.reject { |x| x.strip == "" }.join
    xml
end

resources_folder = File.expand_path("..", Dir.pwd), "modules/services/localization/src/main/res"

languages.each do |language|
    code = language[:code]
    folder = File.join(resources_folder, language[:folder])
    FileUtils.mkdir_p folder

    file = File.join(folder, "strings.xml")

    url = "https://translate.wordpress.com/projects/pocket-casts/android/#{code}/default/export-translations/?format=android"
    puts "Downloading #{url}"
    response = Typhoeus.get(url)
    if response.success?
        xml = clean_xml(response.body)
        write_string_to_file(xml, file)
    else
        puts "Call failed #{response.code}"
    end
end