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


/**
 * Set an assert error and re-start the test so it can fail
 */
function onerror(error)
{
  QUnit.pushFailure(error.message || error, error.stack);

  QUnit.start();
};

_onerror = onerror;

QUnit.jUnitReport = function(report)
{
  // Node.js - write report to file
  if(typeof window === 'undefined')
  {
    var path = './junitResult.xml';

    require('fs').writeFile(path, report.xml, function(error)
    {
      if(error) return console.error(error);

      console.log('XML report saved at ' + path);
    });
  }

  // browser - write report to console
  else
  {
    var textarea = document.getElementById('junit');

    textarea.value = report.xml;
    textarea.style.height = textarea.scrollHeight + "px";
    textarea.style.visibility = "visible";
  }
};

QUnit.lcovReport = function(report)
{
  // Node.js - write report to file
  if(typeof window === 'undefined')
  {
    var path = './lcovResult.xml';

    require('fs').writeFile(path, report.lcov, function(error)
    {
      if(error) return console.error(error);

      console.log('lcov report saved at ' + path);
    });
  }

  // browser - write report to console
  else
  {
    var textarea = document.getElementById('lcov');

    textarea.value = report.lcov;
    textarea.style.height = textarea.scrollHeight + "px";
    textarea.style.visibility = "visible";
  }
};
