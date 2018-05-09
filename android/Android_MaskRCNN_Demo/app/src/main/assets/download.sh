#! /bin/bash
BASE_URL=https://storage.googleapis.com/download.tensorflow.org/models
for MODEL_ZIP in inception5h.zip ssd_mobilenet_v1_android_export.zip stylize_v1.zip speech_commands_conv_actions.zip mobile_multibox_v1.zip
do
  if [ ! -f `pwd`/model/${MODEL_ZIP} ]; then
    echo ${BASE_URL}/${MODEL_ZIP}
    curl -L ${BASE_URL}/${MODEL_ZIP} -o `pwd`/model/${MODEL_ZIP}
    unzip `pwd`/model/${MODEL_ZIP} -d `pwd`
  fi
done
