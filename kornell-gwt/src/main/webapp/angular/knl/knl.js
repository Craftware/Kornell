'use strict';

var app = angular.module('knl', [
    'ngRoute',
    'ui.tree',
    'ngAnimate', 
    'ngSanitize',
    'ui.bootstrap',
    'color.picker',
    'ui.slimscroll',
    'ui.sortable',
    'textAngular',
    'angularFileUpload',
    'angular-intro',
    'ui.bootstrap.datetimepicker'
]);

app.config([
    '$routeProvider', 
    '$locationProvider', 
    '$provide', 
    function($routeProvider, $locationProvider, $provide) {
        $routeProvider.when('/wizard', {
            templateUrl: 'knl/modules/wizard/wizard.html?cache-buster='+(new Date().getTime()),
            controller: 'WizardController'
        })
        .otherwise({
            redirectTo: '/wizard'
        });

        $provide.decorator('ColorPickerOptions', function($delegate) {
            var options = angular.copy($delegate);
            options.inputClass = 'form-control';
            return options;
        });


        $provide.decorator('taOptions', ['taRegisterTool', '$delegate', function(taRegisterTool, taOptions){

            taRegisterTool('fontName', {
                display: "<span class='bar-btn-dropdown dropdown'>" +
                "<button class='btn btn-ta-dropdown dropdown-toggle' type='button' ng-disabled='showHtml()'><i class='fa fa-font'></i><i class='fa fa-caret-down'></i></button>" +
                "<ul class='dropdown-menu'><li ng-repeat='o in options'><button class='btn btn-ta-dropdown checked-dropdown checked-dropdown-list' style='font-size: {{o.css}}; width: 100%' type='button' ng-click='action($event, o.value)'><i ng-if='o.active' class='fa fa-check'></i> {{o.name}}</button></li></ul>" +
                "<ul class='dropdown-menu'><li ng-repeat='o in options'><button class='btn btn-ta-dropdown checked-dropdown checked-dropdown-list' style='font-family: {{o.css}}; width: 100%' type='button' ng-click='action($event, o.css)'><i ng-if='o.active' class='fa fa-check'></i>{{o.name}}</button></li></ul></span>",
                action: function (event, font) {
                    //Ask if event is really an event.
                    if (!!event.stopPropagation) {
                        //With this, you stop the event of textAngular.
                        event.stopPropagation();
                        //Then click in the body to close the dropdown.
                        $("body").trigger("click");
                    }
                    return this.$editor().wrapSelection('fontName', font);
                },
                options: [
                    { name: 'Sans-Serif', css: 'Arial, Helvetica, sans-serif' },
                    { name: 'Serif', css: "'times new roman', serif" },
                    { name: 'Wide', css: "'arial black', sans-serif" },
                    { name: 'Narrow', css: "'arial narrow', sans-serif" },
                    { name: 'Comic Sans MS', css: "'comic sans ms', sans-serif" },
                    { name: 'Courier New', css: "'courier new', monospace" },
                    { name: 'Garamond', css: 'garamond, serif' },
                    { name: 'Georgia', css: 'georgia, serif' },
                    { name: 'Tahoma', css: 'tahoma, sans-serif' },
                    { name: 'Trebuchet MS', css: "'trebuchet ms', sans-serif" },
                    { name: "Helvetica", css: "'Helvetica Neue', Helvetica, Arial, sans-serif" },
                    { name: 'Verdana', css: 'verdana, sans-serif' },
                    { name: 'Proxima Nova', css: 'proxima_nova_rgregular' }
                ]
            });


            taRegisterTool('fontSize', {
                display: "<span class='bar-btn-dropdown dropdown'>" +
                "<button class='btn btn-ta-dropdown dropdown-toggle' type='button' ng-disabled='showHtml()'><i class='fa fa-text-height'></i><i class='fa fa-caret-down'></i></button>" +
                "<ul class='dropdown-menu'><li ng-repeat='o in options'><button class='btn btn-ta-dropdown checked-dropdown checked-dropdown-list' style='font-size: {{o.css}}; width: 100%' type='button' ng-click='action($event, o.value)'><i ng-if='o.active' class='fa fa-check'></i> {{o.name}}</button></li></ul>" +
                "</span>",
                action: function (event, size) {
                    //Ask if event is really an event.
                    if (!!event.stopPropagation) {
                        //With this, you stop the event of textAngular.
                        event.stopPropagation();
                        //Then click in the body to close the dropdown.
                        $("body").trigger("click");
                    }
                    return this.$editor().wrapSelection('fontSize', parseInt(size));
                },
                options: [
                    { name: 'xx-pequena', css: 'xx-small', value: 1 },
                    { name: 'x-pequena', css: 'x-small', value: 2 },
                    { name: 'pequena', css: 'small', value: 3 },
                    { name: 'média', css: 'medium', value: 4 },
                    { name: 'grande', css: 'large', value: 5 },
                    { name: 'x-grande', css: 'x-large', value: 6 },
                    { name: 'xx-grande', css: 'xx-large', value: 7 }

                ]
            });

            // add the button to the default toolbar definition
            taOptions.toolbar[0].push('fontName','fontSize');
            return taOptions;
        }]);
    }
]);

app.run([
    '$rootScope',
    '$location',
    '$http',
    '$timeout',
    function($rootScope, $location, $http, $timeout) {

        var isLocal = window.location.host.indexOf('localhost:') >= 0;
        $rootScope.domain = isLocal ? '*' : parent.location;

        var urlParams = {}
        location.search.substr(1).split("&").forEach(function(item) {urlParams[item.split("=")[0]] = item.split("=")[1]});

        $rootScope.isDebug = urlParams.isDebug || isLocal;


        $rootScope.$on('$locationChangeSuccess', function(next, current) {
            //ga('send', 'pageview', location.hash.substring(2, location.hash.length));
            window.scrollTo(0, 0);
        });

        $rootScope.toasterOptions = {
            'close-button':true, 
            'position-class': 'toast-top-center', 
            'time-out':{ 
                'toast-success': 2500, 
                'toast-warning': 2500, 
                'toast-error': 0 
            } 
        };

        $rootScope.uuid = function() {
          return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
          });
        };

        $rootScope.postMessageToParentFrame = function(messageType, message){
            parent.postMessage(JSON.stringify({type: messageType, message: message}), $rootScope.domain);
        };

        $rootScope.sendKornellNotification = function(notificationType, message){
            parent.postMessage({
                type: "kornellNotification", 
                notificationType: notificationType, 
                message: message
            }, $rootScope.domain);
        };

        if($.cookie("knlLocale") === 'en'){
            moment.locale('en');
        } else {
            moment.locale('pt_BR');
        }
    }
]);