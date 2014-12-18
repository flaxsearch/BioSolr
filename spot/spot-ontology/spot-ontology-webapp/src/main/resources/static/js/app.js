var ontologyApp = angular.module('ontologyApp', ['checklist-model', 'ui.bootstrap', 'ngRoute']);

ontologyApp.config(['$routeProvider', function ($routeProvider) {
	$routeProvider
	.when('/search', {
		templateUrl: '/partials/search.html',
		controller: 'SearchCtrl'
	})
	.when('/sparql', {
		templateUrl: '/partials/sparql.html',
		controller: 'SparqlCtrl'
	})
	.otherwise({
        redirectTo: '/search'
    });
}]);