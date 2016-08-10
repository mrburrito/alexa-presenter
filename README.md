# Alexa Presentation Opener

This code uses Amazon's Echo, driven by a Lambda function, to open and start
a Keynote presentation on a target laptop.

There are two parts to the application, a Java Lambda function that implements
Amazon's Speechlet interface and a Python script that runs on a Mac to open
Keynote and start a presentation.

## Speechlet

The Speechlet reads a list of presentations from a file in S3 and allows
users to ask an Echo to "Start" a presentation or "List" presentations. When
a presentation is started, the Speechlet publishes a message to an SNS topic.
You can subscribe an email address to the topic to see the messages published
by the Speechlet. There must be an SQS queue subscribed to the topic as well.
The messages in the queue are read by the Python script to start the desired
presentation.

### Setup and Configuration

The Speechlet loads its configuration from files in an S3 bucket and publishes messages
to a SNS topic. You will need to create the target SNS topic before running the Lambda
function and, if you want to trigger Keynote on a laptop, create a SQS queue and subscribe
it to the topic. You can set the bucket and keys for the configuration files in the
`S3SessionInitializer` class. The default configuration files, `config/presentations.json` and
`config/topic.txt` are described below.

 
#### `config/presentations.json`

This file contains an array of objects describing the available presentations. Each object
has the following keys:

*   **`name`** _(required)_

    The name of the presentation.
    
*   **`filename`** _(required)_

    The local filename of the presentation. This file will be opened by the Python script
    when the presentation is started.
    
*   **`ssml`** _(optional)_

    Optional SSML instructions telling the Echo how to pronounce the name of the presentation.
    If not provided, the Echo will read the `name` of the presentation.
    
#### `config/topic.txt`

This file should contain the full ARN to the target SNS topic
(e.g. `arn:aws:sns:us-east-1:${ACCOUNT}:bti-presenter`) on a single line.


### Build and Deployment

`gradle build` will generate the archive that is uploaded to AWS Lambda. You can find it
in `build/distributions/alexa-presenter-${VERSION}.zip`. Once created, the code must be
deployed to AWS Lambda.

Sign in to the [AWS Console](https://aws.amazon.com) to complete the following configuration.

#### IAM Role

Lambda requires an IAM Role to execute. You'll need to create a Role that grants your function
permission to access the configuration files in S3 and publish messages to the target SNS topic.
Additionally, your Lambda function will need to be able to create log groups and publish streams
in CloudWatch. The `src/main/aws/sample_policy.json` adds permissions to read from all S3 buckets
and publish to all SNS topics in the current account. You may want to restrict those permissions
to only the bucket and topic relevant to this function in your deployment.

1.  Open the Identity and Access Management Console
2.  Select `Roles`
3.  Click the `Create New Role` button
4.  Enter a name for your role. (e.g. `alexa_presenter_lambda_execution`)
5.  Select `AWS Lambda` as the Role Type
6.  Click `Next Step` without selecting any managed policies
7.  Click `Create Role`
8.  Click on your new Role in the list to open its configuration
9.  Expand the `Inline Policies` section and click on the `click here` link to create a new
    policy.
10. Choose `Custom Policy`
11. Give the policy a name and paste the sample policy or create your own policy in the
    `Policy Document` section.
12. Click `Apply Policy`

#### Lambda Function

Once the IAM Role has been created you can create your function in Lambda.

1.  Open the Lambda Console
2.  Click `Create a Lambda Function`
3.  Click `Skip` on the blueprint page
4.  Select `Alexa Skills Kit` as the trigger for the function; click `Next`
5.  Configure the following options:
    
    -   **Name:** _a function name_
    -   **Description:** _optional description_
    -   **Runtime:** `Java 8`
    -   **Code entry type:** `Upload a .ZIP or JAR file`
    -   **Function package:** `build/distributions/alexa-presenter-${VERSION}.zip`
    -   **Handler:** `com.shankyank.alexa.presenter.PresenterSpeechletRequestStreamHandler`
    -   **Role:** `Choose an existing role`
    -   **Existing role:** `alexa_presenter_lambda_execution` _(select Role created above)_
    -   **Memory (MB):** `256`
    -   **Timeout:** `0` min `30` sec
    -   **VPC:** `No VPC`

6.  Click `Next`
7.  Click `Create function`

#### Alexa

You will need to register a new Skill for Alexa in Amazon's
[developer portal](https://developer.amazon.com) and tie it to the
Lambda function to use this code with an Echo. Your developer account
must use the same login as your Echo to use your skill before it is
published.

1.  Sign in to the [developer console](https://developer.amazon.com)
2.  Click `Alexa` in the Navigation Bar
3.  Click `Get Started` on the `Alexa Skills Kit`
4.  Click the `Add a New Skill` button
5.  Use the following settings:

    -   **Skill Type:** `Custom Interaction Model`
    -   **Name:** _a name_
    -   **Invocation Name:** _the name used to activate the skill_ (e.g. `b.t.i. presenter`)

6.  Click `Next`
7.  Paste the content of `src/main/alexa/presenter_intent_schema.json` in the `Intent Schema`
    section
8.  Click the `Add Slot Type` button
9.  Enter `PRESENTATION_NAME` as the slot `Type`
10. Paste the content of `src/main/alexa/presentation_names.txt` in the `Enter Values` section
11. Paste the content of `src/main/alexa/presenter_utterances.txt` in the `Sample utterances`
    section
12. Click `Next`
13. Enter the following Configuration settings:

    -   **Endpoint:** `Lambda ARN` (enter ARN of your Lambda function in the text box)
    -   **Account Linking:** `No`
14. Click `Next`

You are now ready to test your function. You can use the test interface provided on the
developer portal to see the responses from the Lambda function. At this point, you can
also use the skill from your Echo by saying `"Alexa, tell ${Invocation Name} to Start my presentation".`
Take a look at the `src/main/alexa/presenter_utterances.txt` file to see how you can interact
with the Speechlet.


## Python Script

The Python script is found in `src/main/python/start_presentation.py`. It has been tested with
Python 2.7.12. You should configure a `virutalenv` to run the script so its dependencies don't
conflict with other Python libraries in your system.

The following commands will ensure `virtualenv` is installed, create and activate the virtual
environment `.presenter`, and install all required dependencies. Dependency installation may
take some time as some libraries need to be compiled from source. The commands should be run
from the root directory of the project.

```
pip install virtualenv
virtualenv .presenter && . .presenter/bin/activate
pip install -r requirements.txt
```

Once the dependencies have been installed, run this command to start listening to the SQS queue
for messages from the Presenter Speechlet. The queue name is hard-coded to `start-presentations`;
you can change that by editing the script.

```
python src/main/python/start_presentation.py <PRESENTATION_DIRECTORY>
```

The `PRESENTATION_DIRECTORY` argument should be the path to directory containing the Keynote
presentations described in the `config/presentations.json` file. It can be relative to the
working directory and defaults to the working directory if not provided.
