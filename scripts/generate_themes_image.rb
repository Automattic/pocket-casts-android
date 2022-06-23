#!/usr/bin/env ruby
require 'rubygems'
require 'fileutils'

themes_count = 10
screens_count = 17

def run(command)
    puts command
    `#{command}`
end

run("mkdir -p ~/Downloads/android_themes/")
run("rm -f ~/Downloads/android_themes/*.jpg")
run("rm -f ~/Downloads/android_themes/*.png")

# You may need to run 'adb root' to get access to these screenshots.
run("adb pull /data/user/0/au.com.shiftyjelly.pocketcasts.debug/cache/app_screenshots ~/Downloads/android_themes")
run("mv ~/Downloads/android_themes/app_screenshots/* ~/Downloads/android_themes/")

image_count = 0

Dir.chdir(File.expand_path("~/Downloads/android_themes/")) do
    themes_count.times do |theme_index|
        image_names = ""
        screens_count.times do |screen_index|
            image_count += 1
            image_names += "#{image_count.to_s.rjust(2, '0')}.jpg "
        end
        run("convert #{image_names}-background black -splice 10x10+0+0 -append -chop 10x10+0+0 out_#{theme_index+1}.png")
    end
    run("convert #{Array.new(themes_count) { |e| "out_#{e+1}.png" }.join(" ")} -background white -splice 10x0+0+0 +append -chop 10x0+0+0 out.png")
    run("magick convert out.png -resize 50% themes.png")
end

puts 'open ~/Downloads/android_themes/themes.png'
