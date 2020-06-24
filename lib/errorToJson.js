if ("toJSON" in Error.prototype) {
  return;
}

Object.defineProperty(Error.prototype, "toJSON", {
  value: function () {
    var alt = {};

    Object.getOwnPropertyNames(this).forEach(function (key) {
      alt[key] = this[key];
    }, this);

    return alt;
  },
  configurable: true,
  writable: true,
});
