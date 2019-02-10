'use strict';

var app = angular.module('knlClassroom');

app.controller('FaqLectureController', [
	'$scope',
	'$sce',
	function($scope, $sce) {

		$scope.sce = $sce;

	}
]);
