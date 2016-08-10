import boto3
import json
import sys
import os.path
import applescript


doc_dir = sys.argv[1] if sys.argv[1] else '.'

script = applescript.AppleScript('''
on run argv
    tell application "Keynote"
        activate
        try
            open item 1 of argv
            start the front document
        on error errorMessage number errorNumber
            display alert errorNumber message errorMessage
        end try
    end tell
end run
''')


def start_presentation(presentation):
    presentation_file = os.path.join(doc_dir, presentation['presentation']['filename'])
    print "Starting presentation: %s" % presentation_file
    script.run(presentation_file)


queue = boto3.resource('sqs').get_queue_by_name(QueueName='start-presentations')
while True:
    messages = queue.receive_messages(MaxNumberOfMessages=1, WaitTimeSeconds=20)
    for message in messages:
        start_presentation(json.loads(json.loads(message.body)['Message']))
        message.delete()
