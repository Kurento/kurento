var checkType = require('checktype');

var abstracts = {};
var classes = {};
var complexTypes = {};
var modules = [];

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

function registerComplexTypes(types) {
  for (var name in types) {
    var constructor = types[name]

    // Register constructor checker
    var check = constructor.check;
    if (check) {
      checkType[name] = check;

      // Register constructor
      complexTypes[name] = constructor;
    } else
      checkType[name] = constructor;
  }
}

function registerModule(name) {
  modules.push(name)
  modules.sort()
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
    if (!name) name = constructor.name

    if (name == undefined)
      throw new Error("Can't register an anonymous module");

    registerClass(name, constructor)
  }

  // Registering a plugin
  else {
    if (!name) name = constructor.name

    if (name) registerModule(name)

    for (var key in constructor) {
      var value = constructor[key]

      if (typeof value === 'string') continue

      switch (key) {
      case 'abstracts':
        registerAbstracts(value)
        break

      case 'complexTypes':
        registerComplexTypes(value)
        break

      default:
        registerClass(key, value)
      }
    }
  }
};

module.exports = register;

register.abstracts = abstracts;
register.classes = classes;
register.complexTypes = complexTypes;
register.modules = modules;
