'use strict';

var app = angular.module('knlClassroom');

app.controller('LectureController', [
    '$scope',
    '$rootScope',
    '$timeout',
    '$interval',
    '$location',
    '$window',
    'knlUtils',
    function($scope, $rootScope, $timeout, $interval, $location, $window, knlUtils) {

        $scope.getNodeByUUID = function(uuid){
            var found;
            if($rootScope.classroomInfo.uuid === uuid) found = $rootScope.classroomInfo;
            angular.forEach($rootScope.classroomInfo.modules, function(module){
                if(module.uuid === uuid) found = module;
                angular.forEach(module.lectures, function(lecture){
                    if(lecture.uuid === uuid) found = lecture;
                });
            });
            return found;
        };

        $scope.isLastLecture = function(){
            var lastLecture = false;
            angular.forEach($rootScope.classroomInfo.modules, function(module){
                angular.forEach(module.lectures, function(lecture){
                    lastLecture = (lecture.uuid === $scope.lectureUUID);
                });
            });
            return lastLecture;
        };

        $scope.getFileURL = function(modelAttribute, isRoot){
            var lectureUUID = isRoot ? $scope.classroomInfo.uuid : $scope.lectureUUID;
            var id = lectureUUID + '_' + modelAttribute;
            if($scope.classroomInfo.files[id]){
                var type = $scope.classroomInfo.files[id].type || 'hosted',
                    url = $scope.classroomInfo.files[id][type+"URL"];
                if(type == 'uploaded'){
                    url = $scope.classroomInfo.files._baseURL + url;
                    if(location.hostname === 'localhost'){
                        url = 'http://localhost:8888' + url;
                    }
                }
                return url;
            }
        };

        $scope.verifyNexPrevAvailability = function(){
            var shouldBlockAdvance = function(){
                return $scope.lecture.blockAdvanceDate && ($scope.lecture.blockAdvanceDate >= moment.now().valueOf());
            };

            if(shouldBlockAdvance()){
                knlUtils.enableNext(false);
                var interval = $interval(function(){
                    if(shouldBlockAdvance()){
                        knlUtils.enableNext(false);
                    } else {
                        knlUtils.enableNext(true);
                        $interval.cancel(interval);
                    }
                },3000);
            } else if(knlUtils.getFinalExamJson().isApproved){
                knlUtils.enableNext(true);
                knlUtils.enablePrev(true);
            }
        };

        $scope.initLecture = function(initData){
            if(!initData.classroomInfo) return;

            initializeHideShowControl();

            var classroomInfo = JSON.parse(decodeURI(Base64.decode(initData.classroomInfo)));
            $rootScope.studentName = initData.studentName;

            classroomInfo.lectures = [];
            angular.forEach(classroomInfo.modules, function(module){
                angular.forEach(module.lectures, function(lecture){
                    classroomInfo.lectures.push(lecture);
                });
            });

            $rootScope.classroomInfo = classroomInfo;

            $scope.lectureUUID = $location.$$search.uuid || 0;
            $rootScope.lectureUUID = $scope.lectureUUID;

            $scope.lecture = $scope.getNodeByUUID($scope.lectureUUID);
            $scope.module = $scope.getNodeByUUID($scope.lecture.parentUUID);
            $rootScope.lecture = $scope.lecture;

            $scope.verifyNexPrevAvailability();

            $rootScope.evaluateTimer(true);

            if($scope.classroomInfo.colorBackground){
                $scope.bgStyle = 'background-color: #'+$scope.classroomInfo.colorBackground+';';
            }
            if($scope.getFileURL('imageBackground')){
                $scope.bgStyle += 'background-image: url('+$scope.getFileURL('imageBackground')+');';
            } else if($scope.getFileURL('imageBackground', true)){
                $scope.bgStyle += 'background-image: url('+$scope.getFileURL('imageBackground', true)+');';
            }
            $scope.bgStyle += "background-repeat: no-repeat;" +
                "background-attachment: fixed;" +
                "background-position: center center;";

            angular.forEach($scope.classroomInfo.modules, function(module){
                angular.forEach(module.lectures, function(lecture){
                    if($scope.lectureUUID == lecture.uuid){
                        $scope.module = module;
                    }
                });
            });
        };

        $scope.toggleStructureDebug = function(){
            $scope.isToggleStructureDebug = !$scope.isToggleStructureDebug;
        };

        $rootScope.evaluateTimer = function(isShow){
            if($scope.isShow != isShow){
                $scope.isShow = isShow;
            }
        };

        var initializeHideShowControl = function(){
            var hidden = "hidden";

            // Standards:
            if (hidden in document)
                document.addEventListener("visibilitychange", onchange);
            else if ((hidden = "mozHidden") in document)
                document.addEventListener("mozvisibilitychange", onchange);
            else if ((hidden = "webkitHidden") in document)
                document.addEventListener("webkitvisibilitychange", onchange);
            else if ((hidden = "msHidden") in document)
                document.addEventListener("msvisibilitychange", onchange);
            // IE 9 and lower:
            else if ("onfocusin" in document)
                document.onfocusin = document.onfocusout = onchange;
            // All others:
            else
                window.onpageshow = window.onpagehide
                    = window.onfocus = window.onblur = onchange;

            function onchange (evt) {
                var v = "visible", h = "hidden", isShow = true,
                    evtMap = {
                        focus:v, focusin:v, pageshow:v, blur:h, focusout:h, pagehide:h
                    };

                evt = evt || window.event;

                if (evt.type in evtMap){
                    isShow = evtMap[evt.type] == "visible";
                } else {
                    isShow = !this[hidden];
                }

                var scope = angular.element(document.getElementById('knlClassroomApp')).scope();
                scope.evaluateTimer(isShow);
            }

            // set the initial state (but only if browser supports the Page Visibility API)
            if( document[hidden] !== undefined )
                onchange({type: document[hidden] ? "blur" : "focus"});
        };

        if(window.isPreview) {
            var initData = {
                classroomInfo: localStorage.KNLwp,
                studentName: $location.$$search.studentName
            };
            $scope.initLecture(initData);
        } else {
            $scope.postMessageToParentFrame("classroomReady", "");
            $window.addEventListener('message',function(event) {
                $scope.$apply(function(){
                    var messageData = JSON.parse(event.data);
                    if(messageData.type === 'initializeContents'){
                        $scope.initLecture(JSON.parse(messageData.message));
                    }
                });
            },false);
        }

    }
]);
