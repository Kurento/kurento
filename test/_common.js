/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

const REPORTS_DIR = 'reports'

function writeReport(ext, data) {
  var path = REPORTS_DIR + '/' + require('../package.json').name + '.' + ext

  require('fs-extra').outputFile(path, data, function (error) {
    if (error) return console.trace(error);

    console.log(ext + ' report saved at ' + path);
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
