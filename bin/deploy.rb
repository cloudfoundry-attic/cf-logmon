#!/usr/bin/env ruby

require 'tempfile'
require 'yaml'
require 'securerandom'

Dir.chdir(File.expand_path('../', __dir__)) do
  puts 'Building application locally ... this may take a while.'
  system './gradlew clean assemble -Dorg.gradle.project.version=1.0.x > /dev/null'
end

username = 'admin'
password = SecureRandom.uuid

application_name = ARGV[0] || 'logmon'

manifest = {
  'applications' => [{
    'name' => application_name,
    'path' => File.expand_path('../build/libs/logmon-1.0.x.jar', __dir__),
    'env' => {
      'LOGMON_AUTH_USERNAME' => username,
      'LOGMON_AUTH_PASSWORD' => password,
    }
  }]
}

Tempfile.open('manifest') do |f|
  f.write YAML.dump(manifest)
  f.close

  puts 'Deploying application'
  system "cf push -f #{f.path} > /dev/null"
end

puts <<-NOTICE

********************************************************************************

The application deployed successfully!

You can now visit the UI at: https://#{`cf app #{application_name} | grep routes | awk '{print $2}'`.chomp}

Your username and password are:

Username: #{username}
Password: #{password}

If you want to change your username/password, you can run the following commands:

cf set-env #{application_name} LOGMON_AUTH_USERNAME $new_username
cf set-env #{application_name} LOGMON_AUTH_PASSWORD $new_password

********************************************************************************

NOTICE
