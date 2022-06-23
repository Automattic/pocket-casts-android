require "google/apis/sheets_v4"
require "googleauth"
require "googleauth/stores/file_token_store"
require "fileutils"

OOB_URI = "urn:ietf:wg:oauth:2.0:oob".freeze
APPLICATION_NAME = "Google Sheets API Pocket Casts Themes".freeze
CREDENTIALS_PATH = "google_credentials.json".freeze
# The file token.yaml stores the user's access and refresh tokens, and is
# created automatically when the authorization flow completes for the first
# time.
TOKEN_PATH = "token.yaml".freeze
SCOPE = Google::Apis::SheetsV4::AUTH_SPREADSHEETS_READONLY

class String
  def uncapitalize
    self[0, 1].downcase + self[1..-1]
  end
end

def authorize()
  client_id = Google::Auth::ClientId.from_file CREDENTIALS_PATH
  token_store = Google::Auth::Stores::FileTokenStore.new file: TOKEN_PATH
  authorizer = Google::Auth::UserAuthorizer.new client_id, SCOPE, token_store
  user_id = "default"
  credentials = authorizer.get_credentials user_id
  if credentials.nil?
    url = authorizer.get_authorization_url base_url: OOB_URI
    puts "Open the following URL in the browser and enter the " \
         "resulting code after authorization:\n" + url
    code = gets
    credentials = authorizer.get_and_store_credentials_from_code(
      user_id: user_id, code: code, base_url: OOB_URI
    )
  end
  credentials
end

def response_to_tokens_map(response)
  tokens = []
  response.values.each do |row|
    key = row[0]
    next if key.nil? || key.length == 0
    tokens << {
      key: key.gsub("$", "").gsub("-", "_"),
      token_name: key,
      kotlin_name: key.gsub("$", "").split('-').collect(&:capitalize).join.uncapitalize,
      themes: {
        light: {
          hex: row[2],
          opacity: row[3]
        },
        dark: {
          hex: row[4],
          opacity: row[5]
        },
        extra_dark: {
          hex: row[6],
          opacity: row[7]
        },
        classic_light: {
          hex: row[8],
          opacity: row[9]
        },
        classic_dark: {
          hex: row[10],
          opacity: row[11]
        },
        electricity: {
          hex: row[12],
          opacity: row[13]
        },
        indigo: {
          hex: row[14],
          opacity: row[15]
        },
        radioactive: {
          hex: row[16],
          opacity: row[17]
        },
        rose: {
          hex: row[18],
          opacity: row[19]
        },
        light_contrast: {
          hex: row[20],
          opacity: row[21]
        },
        dark_contrast: {
          hex: row[22],
          opacity: row[23]
        }
      }
    }
  end
  tokens.each do |token_attrs|
      token_attrs[:user_input] = !token_attrs[:themes].select { |key,value| value[:hex] == "$podcast" || value[:hex] == "$filter" }.empty?
  end
  return tokens
end

def download_themes()
  if !File.exists?(CREDENTIALS_PATH)
    puts "Download Google Sheet credentials 'google_credentials.json' from: https://developers.google.com/sheets/api/quickstart/ruby"
    return nil
  end

  # Initialize the API
  service = Google::Apis::SheetsV4::SheetsService.new
  service.client_options.application_name = APPLICATION_NAME
  service.authorization = authorize

  # https://docs.google.com/spreadsheets/d/1BZWwQo8ZhTt9jRz5eX6iJqt4r9o6ekWeYi_t8AlNDCM/edit
  spreadsheet_id = "1BZWwQo8ZhTt9jRz5eX6iJqt4r9o6ekWeYi_t8AlNDCM"
  range = "A3:AZ200"
  response = service.get_spreadsheet_values spreadsheet_id, range
  puts "No data found." if response.values.empty?

  return response_to_tokens_map(response)
end