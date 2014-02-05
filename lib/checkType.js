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

function checkArray(type, key, value)
{
  if(!(value instanceof Array))
    throw SyntaxError(key+' param should be an Array');
  for(var i=0, item; item=value[i]; i++)
    this[type](key+'['+i+']', item);
};

function checkBoolean(key, value)
{
  if(typeof value != "boolean")
    throw SyntaxError(key+" param should be a Boolean");
};

function checkDouble(key, value)
{
  if(typeof value != 'number')
    throw SyntaxError(key+" param should be a Float");
};

function checkInteger(key, value)
{
  if(!Number.isInteger(value))
    throw SyntaxError(key+" param should be an Integer");
};

function checkObject(key, value)
{
  if(typeof value != 'object')
    throw SyntaxError(key+' param should be an Object');
};

function checkString(key, value)
{
  if(typeof value != "string")
    throw SyntaxError(key+" param should be a String");
};


//
// Kurento specific ones
//

function checkKmsMediaChromaBackgroundImage(key, value)
{
  checkString(key+'.uri', value.uri, true);
};

function checkKmsMediaChromaColorCalibrationArea(key, value)
{
  checkInteger(key+'.x',      value.x,      true);
  checkInteger(key+'.y',      value.y,      true);
  checkInteger(key+'.width',  value.width,  true);
  checkInteger(key+'.height', value.height, true);
};


function checkMediaMuxer(key, value)
{
  if(typeof value != 'string')
    throw SyntaxError(key+' param should be a String');
  if(!value.match('WEBM|MP4'))
    throw SyntaxError(key+' param is not one of WEBM or MP4');
};

function checkMediaPointerDetectorWindow(key, value)
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

function checkMediaPointerDetectorWindowSet(key, value)
{
  checkSet_MediaPointerDetectorWindow(key+'.windows', value.windows);
};

function checkMediaProfile(key, value)
{
  checkObject(key, value);

  checkMediaMuxer(key+'.mediaMuxer', value.mediaMuxer);
};


function checkSet_MediaPointerDetectorWindow(key, value)
{
  checkArray('MediaPointerDetectorWindow', key, value);
};


// Checker function

function checkType(type, key, value, required)
{
  if(required && !value)
    throw SyntaxError(key+" param is required");

  checkType[type](key, value);
};


module.exports = checkType;

exports.boolean = checkBoolean;
exports.double  = checkDouble;
exports.integer = checkInteger;
exports.Object  = checkObject;
exports.String  = checkString;

exports.KmsMediaChromaBackgroundImage      = checkKmsMediaChromaBackgroundImage;
exports.KmsMediaChromaColorCalibrationArea = checkKmsMediaChromaColorCalibrationArea;

exports.MediaMuxer                    = checkMediaMuxer;
exports.MediaPointerDetectorWindow    = checkMediaPointerDetectorWindow;
exports.MediaPointerDetectorWindowSet = checkMediaPointerDetectorWindowSet;
exports.MediaProfile                  = checkMediaProfile;

exports.Set_MediaPointerDetectorWindow = checkSet_MediaPointerDetectorWindow;