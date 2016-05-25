var checkType = require('./checkType');

var abstracts = {};
var classes = {};
var complexTypes = {};
var modules = [];

function registerAbstracts(classes, hierarchy) {
  for (var name in classes) {
    var constructor = classes[name]

    // Register constructor checker
    var check = constructor.check;
    if (check) checkType[name] = check;

    // Register constructor
    abstracts[name] = constructor;
    abstracts[hierarchy + "." + name] = constructor;
  }
}

function registerClass(name, constructor) {
  // Register constructor checker
  var check = constructor.check;
  if (check) checkType[name] = check;

  // Register constructor
  classes[name] = constructor;
}

function registerComplexTypes(types, hierarchy) {
  for (var name in types) {
    var constructor = types[name]

    // Register constructor checker
    var check = constructor.check;
    if (check) {
      checkType[name] = check;
      checkType[hierarchy + "." + name] = check;

      // Register constructor
      complexTypes[name] = constructor;
      complexTypes[hierarchy + "." + name] = constructor;
    } else {
      checkType[name] = constructor;
      checkType[hierarchy + "." + name] = constructor;
    }
  }
}

function registerModule(name) {
  modules.push(name)
  modules.sort()
}

function register(name, constructor) {
  // Adjust parameters
  if (!name)
    throw SyntaxError('Need to define an object, a module or a function')

  if (typeof name != 'string') {
    constructor = name
    name = undefined
  }

  // Execute require if we only have a name
  if (constructor == undefined)
    return register(require(name));

  // Execute require if the constructor is set as a string
  if (typeof constructor === 'string')
    return register(name, require(constructor));

  // Registering a function
  if (constructor instanceof Function) {
    // Registration name
    if (!name) name = constructor.name

    if (name == undefined)
      throw new SyntaxError("Can't register an anonymous module");

    return registerClass(name, constructor)
  }

  // Registering a plugin
  if (!name) name = constructor.name

  if (name) registerModule(name)

  for (var key in constructor) {
    var value = constructor[key]

    if (name === 'core' || name === 'elements' || name === 'filters')
      name = 'kurento'
    var hierarchy = name + "." + key;

    if (typeof value !== 'string')
      switch (key) {
      case 'abstracts':
        registerAbstracts(value, name)
        break

      case 'complexTypes':
        registerComplexTypes(value, name)
        break

      default:
        registerClass(hierarchy, value)
        registerClass(key, value)
      }
  }
};

module.exports = register;

register.abstracts = abstracts;
register.classes = classes;
register.complexTypes = complexTypes;
register.modules = modules;
