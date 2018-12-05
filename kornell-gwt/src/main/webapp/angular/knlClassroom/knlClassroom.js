'use strict';

var app = angular.module('knlClassroom', [
    'ngRoute',
    'ngSanitize',
    'pascalprecht.translate'
]);

app.config([
    '$routeProvider', 
    '$locationProvider', 
    '$translateProvider',
    function($routeProvider, $locationProvider, $translateProvider) {
        $routeProvider.when('/lecture', {
            templateUrl: 'modules/lecture/lecture.html?cache-buster=1.5',
            controller: 'LectureController'
        })
        .otherwise({
            redirectTo: '/lecture'
        });

        var setupInternationalization = function(){
            var translations_en_keys = $.map(translations_en, function(value, key) {
              return key;
            });

            var translations_pt_BR_keys = $.map(translations_pt_BR, function(value, key) {
              return key;
            });

            var diff_pt_BR = $(translations_en_keys).not(translations_pt_BR_keys).get(),
                diff_en = $(translations_pt_BR_keys).not(translations_en_keys).get();

            if(diff_pt_BR.length){
                console.error('Missing translations in pt_BR:', diff_pt_BR.join(', '));
            }
            if(diff_en.length){
                console.error('Missing translations in en:', diff_en.join(', '));
            }
            
            $translateProvider.useSanitizeValueStrategy('sanitize');
            $translateProvider.translations('en', translations_en);
            $translateProvider.translations('pt_BR', translations_pt_BR);
            if($.cookie("knlLocale") === 'en'){
                $translateProvider.preferredLanguage('en');
            } else {
                $translateProvider.preferredLanguage('pt_BR');
            }
        }();
}]);

app.run([
    '$rootScope',
    '$location',
    '$http',
    '$timeout',
    function($rootScope, $location, $http, $timeout) {

        var isLocal = window.location.host.indexOf('localhost:') >= 0;
        $rootScope.domain = isLocal ? '*' : parent.location;

        if($location.$$search && $location.$$search.preview === "1"){
            window.isPreview = true;
        }
        
        $rootScope.isDebug = (window.location.host.indexOf("localhost") === 0);
        
        $rootScope.$on('$locationChangeSuccess', function(next, current) {
            //ga('send', 'pageview', location.hash.substring(2, location.hash.length));
            window.scrollTo(0, 0);
        });

        $rootScope.postMessageToParentFrame = function(messageType, message){
            parent.postMessage(JSON.stringify({type: messageType, message: message}), $rootScope.domain);
        };

    }
]);