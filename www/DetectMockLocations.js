var exec = require('cordova/exec');

module.exports.coolMethod = function(arg0, success, error) {
    exec(success, error, 'DetectMockLocations', 'areThereMockPermissionApps', [arg0]);
};