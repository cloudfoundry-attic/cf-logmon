#!/usr/bin/env ruby

require 'tempfile'
require 'yaml'
require 'securerandom'

trap 'INT' do
  puts
  puts "Bye bye..."
  exit
end

puts <<-USER_CREATION

********************************************************************************

 ▄████▄    █████▒       ██▓     ▒█████    ▄████  ███▄ ▄███▓ ▒█████   ███▄    █
▒██▀ ▀█  ▓██   ▒       ▓██▒    ▒██▒  ██▒ ██▒ ▀█▒▓██▒▀█▀ ██▒▒██▒  ██▒ ██ ▀█   █
▒▓█    ▄ ▒████ ░       ▒██░    ▒██░  ██▒▒██░▄▄▄░▓██    ▓██░▒██░  ██▒▓██  ▀█ ██▒
▒▓▓▄ ▄██▒░▓█▒  ░       ▒██░    ▒██   ██░░▓█  ██▓▒██    ▒██ ▒██   ██░▓██▒  ▐▌██▒
▒ ▓███▀ ░░▒█░          ░██████▒░ ████▓▒░░▒▓███▀▒▒██▒   ░██▒░ ████▓▒░▒██░   ▓██░
░ ░▒ ▒  ░ ▒ ░          ░ ▒░▓  ░░ ▒░▒░▒░  ░▒   ▒ ░ ▒░   ░  ░░ ▒░▒░▒░ ░ ▒░   ▒ ▒
  ░  ▒    ░            ░ ░ ▒  ░  ░ ▒ ▒░   ░   ░ ░  ░      ░  ░ ▒ ▒░ ░ ░░   ░ ▒░
░         ░ ░            ░ ░   ░ ░ ░ ▒  ░ ░   ░ ░      ░   ░ ░ ░ ▒     ░   ░ ░
░ ░                        ░  ░    ░ ░        ░        ░       ░ ░           ░
░                                                                            2.0
********************************************************************************

Welcome to CF-Logmon, we're about to do some setup. While you wait,
you may wish to create a user with the space-auditor role for us to act as.
As a reminder, this user is specifically intended to read cf-logmon logs.
Moreover, this user's credentials will be accessible in the CF environment.
Therefore, it should not be you or any other human user.

An example set of commands to create such a user:

   cf create-user <username> <password>
   cf set-space-role <username> <org> <space> SpaceAuditor

Note: keep track of the username and password. We will you ask you for these credentials in a bit.

USER_CREATION

Dir.chdir(File.expand_path('../', __dir__)) do
  puts 'Building application locally.'
  system 'GOOS=linux go build -o bin/cf-logmon'
end

username = 'admin'
password = SecureRandom.uuid

application_name = ARGV[0] || 'logmon'

puts <<-ABOUT_TO_PUSH

********************************************************************************

THE TIME HAS COME!

We're about to push cf-logmon to this target:

#{`cf target`}

If any of the above information does not look correct, please hit CTRL+C now.

Please enter the credentials you created earlier below.
Note: they will be echoed in plaintext - this is to reinforce that the credentials should be ephemeral
and not belong to a real user.

ABOUT_TO_PUSH

print "Space Auditor Username: "
log_reader_username = $stdin.gets.chomp

print "Space Auditor Password: "
log_reader_password = $stdin.gets.chomp

print "Log Messages Per Test: (default: 1000) "
log_messages = $stdin.gets.chomp

print "Log Byte Size: (default: 256) "
log_byte_size = $stdin.gets.chomp

print "Emit Duration Per Test: (default: 1s) "
emit_duration = $stdin.gets.chomp

print "Run Interval: (default: 5m) "
run_interval = $stdin.gets.chomp

print "Log Transit Wait Per Test: (default: 10s) "
log_transit_wait = $stdin.gets.chomp

print "Skip SSL Validation? [y/N] "
skip_ssl = $stdin.gets.chomp

skip_validate_ssl = false
if skip_ssl == "y"
  skip_validate_ssl = true
end

puts

manifest = {
  'applications' => [{
    'name' => application_name,
    'path' => File.expand_path('..', __dir__),
    'command' => './bin/cf-logmon',
    'buildpacks' => ['binary_buildpack'],
    'env' => {
      'LOGMON_AUTH_USERNAME' => username,
      'LOGMON_AUTH_PASSWORD' => password,
      'LOGMON_CONSUMPTION_USERNAME' => log_reader_username,
      'LOGMON_CONSUMPTION_PASSWORD' => log_reader_password,
      'LOG_MESSAGES_PER_BATCH' => log_messages,
      'LOG_SIZE_BYTES' => log_byte_size,
      'BATCH_EMIT_DURATION' => emit_duration,
      'LOG_TRANSIT_WAIT' => log_transit_wait,
      'RUN_INTERVAL' => run_interval,
      'SKIP_CERT_VERIFY' => skip_validate_ssl,
    }
  }]
}
puts YAML.dump(manifest)
Tempfile.open('manifest') do |f|
  f.write YAML.dump(manifest)
  f.close

  puts 'Deploying application ...'
  system "cf push -f #{f.path}"
end

puts <<-NOTICE

********************************************************************************

The application deployed successfully!

You can now visit the UI at: https://#{`cf app #{application_name} | grep routes | awk '{print $2}'`.chomp}

The UI is protected with HTTP Basic Auth. The username and password are:

Username: #{username}
Password: #{password}

If you want to change these credentials, run the following commands:

cf set-env #{application_name} LOGMON_AUTH_USERNAME <new username>
cf set-env #{application_name} LOGMON_AUTH_PASSWORD <new password>

********************************************************************************

NOTICE
