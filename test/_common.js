/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

const REPORTS_DIR = 'reports'

function writeReport(ext, data) {
  var path = REPORTS_DIR + '/' + require('../package.json').name + '.' + ext

  require('fs-extra').outputFile(path, data, function (error) {
    if (error) return console.debug(error);

    console.debug(ext + ' report saved at ' + path);
  });
}

function fetchReport(type, report) {
  var ext = type
  if (type == 'junit') ext = 'xml'

  report = report[ext]

  // Node.js - write report to file
  if (typeof window === 'undefined')
    writeReport(ext, report)

  // browser - write report to console
  else {
    var textarea = document.getElementById(type);

    textarea.value = report;
    textarea.style.height = textarea.scrollHeight + "px";
    textarea.style.visibility = "visible";
  }
}

QUnit.jUnitReport = fetchReport.bind(undefined, 'junit')
QUnit.lcovReport = fetchReport.bind(undefined, 'lcov')
