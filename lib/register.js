var checkType = require('checktype');

var abstracts = {};
var classes = {};

function registerAbstracts(classes) {
  for (var name in classes) {
    var constructor = classes[name]

    // Register constructor checker
    var check = constructor.check;
    if (check) checkType[name] = check;

    // Register constructor
    abstracts[name] = constructor;
  }
}

function registerClass(name, constructor) {
  // Register constructor checker
  var check = constructor.check;
  if (check) checkType[name] = check;

  // Register constructor
  classes[name] = constructor;
}

function registerComplexTypes(complexTypes) {
  for (var name in complexTypes)
    checkType[name] = complexTypes[name];
}

function register(name, constructor) {
  // Adjust parameters
  if (typeof name != 'string') {
    constructor = name
    name = undefined
  } else if (constructor == undefined) {
    // Execute require if we only have a name
    return register(name, require(name));
  }

  // Registering a function
  if (constructor instanceof Function) {
    // Registration name
    if (!name)
      name = constructor.name

    if (name == undefined)
      throw new Error("Can't register an anonymous module");

    registerClass(name, constructor)
  }

  // Registering a plugin
  else
    for (key in constructor)
      switch (key) {
      case 'abstracts':
        registerAbstracts(constructor[key])
        break

      case 'complexTypes':
        registerComplexTypes(constructor[key])
        break

      default:
        registerClass(key, constructor[key])
      }
};

module.exports = register;

register.abstracts = abstracts;
register.classes = classes;
