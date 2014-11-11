ontologyApp.controller('OntologyPageCtrl', ['$scope', '$http', function($scope, $http) {
	
	var self = this;
	
	// Event handler to catch search form submit
	$scope.search = function() {
		var params = { q: $scope.query };
		
		$http({
			method: 'GET',
			url: '/service/search',
			params: params
		}).success(function(data) {
			if (data.error) {
				if (window.console) {
					console.log(data.errorMessage);
				}
				
				// Error state - show an error message, hide the search results
				$scope.error = 'A server error has occurred - try again later.';
			} else {
				$scope.results = data.results;
				$scope.total = data.totalResults;
				$scope.start = data.start + 1;
				$scope.end = data.start + data.rows;
			}
		});
	}
	
}]);