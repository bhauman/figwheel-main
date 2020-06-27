require 'kramdown'
require 'kramdown-parser-gfm'
require 'kramdown-syntax-coderay'

for filename in ARGV do
  contents = File.read(filename)
  doc = Kramdown::Document.new(contents, {input: 'GFM',
                                          hard_wrap: false,
                                          syntax_highlighter: :coderay,
                                          coderay_line_numbers: nil})
  new_name = filename.split("/").last.split(".").first + ".html"
  content = "<!-- DO NOT EDIT: File generated from Markdown source found in #{filename} -->\n" + doc.to_html

  File.write('helper-resources/public/com/bhauman/figwheel/helper/content/' + new_name , content)
end
