/**
 * Number.isInteger() polyfill
 * @function external:Number#isInteger
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/isInteger Number.isInteger}
 */
if (!Number.isInteger) {
  Number.isInteger = function isInteger (nVal) {
    return typeof nVal === "number" && isFinite(nVal) && nVal > -9007199254740992 && nVal < 9007199254740992 && Math.floor(nVal) === nVal;
  };
}


//
// Basic types
//

function checkArray(type, key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  if(!(value instanceof Array))
    throw SyntaxError(key+' param should be an Array');
  for(var i=0, item; item=value[i]; i++)
    this[type](key+'['+i+']', item);
};

function checkBoolean(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  if(typeof value != "boolean")
    throw SyntaxError(key+" param should be Boolean");
};

function checkDouble(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  if(typeof value != 'number')
    throw SyntaxError(key+" param should be Float");
};

function checkInteger(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  if(!Number.isInteger(value))
    throw SyntaxError(key+" param should be Integer");
};

function checkString(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  if(typeof value != "string")
    throw SyntaxError(key+" param should be String");
};


//
// Kurento specific ones
//

function checkKmsMediaChromaBackgroundImage(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  checkString(key+'.uri', value.uri, true);
};

function checkKmsMediaChromaColorCalibrationArea(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  checkInteger(key+'.x',      value.x,      true);
  checkInteger(key+'.y',      value.y,      true);
  checkInteger(key+'.width',  value.width,  true);
  checkInteger(key+'.height', value.height, true);
};


function checkMediaMuxer(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  if(typeof value != 'string')
    throw SyntaxError(key+' param should be a String');
  if(!value.match('WEBM|MP4'))
    throw SyntaxError(key+' param is not one of WEBM or MP4');
};

function checkMediaPointerDetectorWindow(key, value, mandatory)
{
  checkInteger(key+'.topRightCornerX', value.topRightCornerX, true);
  checkInteger(key+'.topRightCornerY', value.topRightCornerY, true);
  checkInteger(key+'.width',           value.width,           true);
  checkInteger(key+'.height',          value.height,          true);
  checkString (key+'.id',              value.id,              true);

  checkString(key+'.inactiveOverlayImageUri', value.inactiveOverlayImageUri);
  checkDouble(key+'.overlayTransparency',     value.overlayTransparency);
  checkString(key+'.activeOverlayImageUri',   value.activeOverlayImageUri);
};

function checkMediaPointerDetectorWindowSet(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  checkSet_MediaPointerDetectorWindow(key+'.windows', value.windows);
};

function checkMediaProfile(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  if(typeof value != 'object')
    throw SyntaxError(key+' param should be an Object');

  checkMediaMuxer(key+'.mediaMuxer', value.mediaMuxer);
};


function checkSet_MediaPointerDetectorWindow(key, value, mandatory)
{
  if(mandatory && !value)
    throw SyntaxError(key+" param is mandatory");

  checkArray('MediaPointerDetectorWindow', key, value);
};


exports.boolean = checkBoolean;
exports.double  = checkDouble;
exports.integer = checkInteger;
exports.String  = checkString;

exports.KmsMediaChromaBackgroundImage      = checkKmsMediaChromaBackgroundImage;
exports.KmsMediaChromaColorCalibrationArea = checkKmsMediaChromaColorCalibrationArea;

exports.MediaMuxer                    = checkMediaMuxer;
exports.MediaPointerDetectorWindow    = checkMediaPointerDetectorWindow;
exports.MediaPointerDetectorWindowSet = checkMediaPointerDetectorWindowSet;
exports.MediaProfile                  = checkMediaProfile;

exports.Set_MediaPointerDetectorWindow = checkSet_MediaPointerDetectorWindow;