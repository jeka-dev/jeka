#!/bin/bash

#
# Copyright 2014-2024  the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
#  limitations under the License.
#

set -e

# Script for installing and upgrading JeKa
# Authors: Jerome Angibaud

# curl -s https://raw.githubusercontent.com/jeka-dev/jeka/0.11.x/dev.jeka.core/src/main/shell/jeka-install | $(echo $0) -s - setup


declare MAVEN_REPO="https://repo1.maven.org/maven2"
declare MANUAL_NOTICE="https://jeka-dev.github.io/jeka/reference-guide/installation"


download_and_unpack() {
  local url=$1
  local dir=$2
  local file_type=$3  # 'zip' or 'tar.gz'
  local temp_file
  temp_file=$(mktemp)
  rm "$temp_file"

  ## download
  if [ -x "$(command -v curl)" ]; then
    curl -sLf --fail --show-error -o "$temp_file" "$url"
  elif [ -x "$(command -v wget)" ]; then
    wget -q -O "$temp_file" "$url"
  else
    echo "Error: curl or wget not found, please make sure one of them is installed" 1>&2
    exit 1
  fi

  ## unpack
  if [ "$file_type" == "tar.gz" ]; then
      mkdir -p "$dir"
      gzip -cd "$temp_file" | tar xf - -C "$dir"
  else
    unzip -q -o -j "$temp_file" -d "$dir" -x '__MACOSX/*'
  fi
  rm "$temp_file"
}

check_prerequisites() {
  if ! command -v git > /dev/null 2>&1; then
      echo "Git is not installed. Please install it prior installing Jeka"
      exit 1
  fi
  if ! command -v curl > /dev/null 2>&1; then
      echo "Curl is not installed. Please install it prior installing Jeka"
      exit 1
  fi
  if ! command -v unzip  > /dev/null 2>&1; then
      echo "Unzip is not installed. Please install it prior installing Jeka"
      exit 1
  fi
}

compute_LAST_VERSION() {
  local group_id="dev.jeka"
  local artifact_id="jeka-core"
  local url
  url="http://search.maven.org/solrsearch/select?q=g:$group_id+AND+a:$artifact_id&rows=1&wt=json"
  LAST_VERSION=$(curl -sL "$url" | sed -n 's|.*"latestVersion":"\([^"]*\)".*|\1|p')
}

compute_LAST_RELEASE() {
    response=$(curl -s "https://api.github.com/repos/jeka-dev/jeka/tags")

    # Extract and print the name of the last release without '-' character
    latest_release=$(echo "$response" | jq -r '.[].name' | grep -v -- '-' | head -n 1)
    LAST_RELEASE=$latest_release
}

get_download_url() {
  local version="$1"
  echo "$MAVEN_REPO/dev/jeka/jeka-core/$version/jeka-core-$version-distrib.zip"
}

compute_SHELL_CONFIG_FILE() {
  local shell_bin
  local current_shell="$SHELL"
  if [ "$current_shell" == "" ]; then
    current_shell="$0"
  fi
  if [[ $current_shell == *"bash"* ]]; then
      shell_bin="bash"
  elif [[ $current_shell == *"zsh"* ]]; then
      shell_bin="zsh"
  fi
  if [ "$shell_bin" == "zsh" ]; then
    SHELL_CONFIG_FILE="$HOME/.zshrc"
  elif [ -f "$HOME/.bashrc" ]; then
       SHELL_CONFIG_FILE="$HOME/.bashrc"
  elif [ -f "$HOME/.bash_profile" ]; then
      SHELL_CONFIG_FILE="$HOME/.bash_profile"
  elif [ -f "$HOME/.profile" ]; then
      SHELL_CONFIG_FILE="$HOME/.profile"
  fi
}

## install in the current directory
## This method is intended to install JeKa from scratch
setup() {

  check_prerequisites

  compute_SHELL_CONFIG_FILE

  # First check if Jeka is not already installed
  local current_jeka_path
  current_jeka_path=$(which jeka || echo "not found")
  if [ "$current_jeka_path" != "not found" ]; then
    echo "Your system seems to already have JeKa installed at : $current_jeka_path"
    echo "If it is a an old version of jeka (<0.11), you can rename it (i.e. jekal) and re-run this script."
    echo
    echo "If it is an recent version, you can update it by executing 'jeka-install' or 'jeka-install <version>'."
    echo "You can see latest versions here : https://central.sonatype.com/artifact/dev.jeka/jeka-core/versions"
    echo ""
    echo "For manual install/fixing, check your $SHELL_CONFIG_FILE file"
    echo "and visit manual installation guide et ; $MANUAL_NOTICE"
    echo ""
    exit 1
  fi

  # Check if we can determine shell configuration file for augmenting PATH
  if [ "$SHELL_CONFIG_FILE" == "" ]; then
    echo "Your shell config can't be determined, please $MANUAL_NOTICE" 2>&1
    exit 1
  fi

  # Proceed install
  compute_LAST_VERSION
  if [ "$SPECIFIC_JEKA_DIST_URL" != "" ]; then  ## $SPECIFIC_JEKA_DIST_URL may be overridden
    url="$SPECIFIC_JEKA_DIST_URL"
  else
    local url
    url="$(get_download_url "$LAST_VERSION")"
  fi

  # Downloading and unpack
  local install_dir="$HOME/.jeka"
  mkdir -p $install_dir
  touch "$HOME/.jeka/global.properties"
  echo "Installing Jeka Version $LAST_VERSION in $install_dir ..."
  download_and_unpack "$url" "$install_dir" "zip"
  chmod +x "$install_dir/jeka"
  chmod +x "$install_dir/jeka-install"

  # Adapt shell script
  compute_SHELL_CONFIG_FILE
  echo "Append JEKA_HOME to PATH in $SHELL_CONFIG_FILE"
  local jeka_path
  echo "" >> "$SHELL_CONFIG_FILE"
  echo "# Setting Jeka HOME" >> "$SHELL_CONFIG_FILE"
  echo "export PATH=\$PATH:$install_dir" >> "$SHELL_CONFIG_FILE"

  echo "Checking install with 'jeka --version'. This requires JDK download. It may take a while ..."
  export PATH=$PATH:$install_dir
  jeka --version
  echo "Jeka is properly installed."
  echo "Later on, you can upgrade to a different JeKa version by running either 'jeka-install' or 'jeka-install <version>'."
}

update() {
  local version="$1"
  if [ "$version" == "" ]; then
    compute_LAST_VERSION
    version="$LAST_VERSION"
    #compute_LAST_RELEASE
    #version="$LAST_RELEASE"
  fi
  echo "Updating Jeka to version $version ? [y/n]"
  read -r user_input
  if [ "$user_input" != "y" ]; then
    exit 1
  fi
  echo "Updating to Jeka version $version ..."
  local url
  url="$(get_download_url "$version" || "")"
  local jeka_path
  jeka_path=$(which jeka || echo "")
  if [ "$jeka_path" == "" ]; then
    echo "Can't find either 'jeka' executable nor \$JEKA_HOME environment variable."
    echo "Are you sure JeKa is properly installed on your system ?"
    exit 1
  fi
  download_and_unpack "$url" "$jeka_path" "zip"
  echo "Jeka updated to version $version"
}

# ------------------------------- Script start here ----------------------

if [ "$1" == "setup" ]; then  ## hidden functionality used only for installing from scratch
  setup
else
  echo "Update installed Jeka Version. Usage 'jeka-install <version>'."
  echo "If version is not specified, last version is used."
  echo "Too see latest available versions, visit https://central.sonatype.com/artifact/dev.jeka/jeka-core/versions"
  echo ""
  update "$1"
fi

