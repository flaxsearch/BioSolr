ontologyApp.controller('OntologyPageCtrl', ['$scope', '$http', function($scope, $http) {
	
	var self = this;
	
	self.init = function() {
		// Look up the dynamic label fields
		$http({
			method: 'GET',
			url: '/service/dynamicLabelFields'
		}).success(function(data) {
			$scope.dynamicLabels = data;
		});
		
		$scope.additionalFields = [];
		// Kludge so we can use checkbox-model for the parent/child label checkboxes
		$scope.efo_child_labels = 'efo_child_labels';
		$scope.efo_parent_labels = 'efo_parent_labels';
	}
	
	self.logError = function(errorMsg) {
		if (window.console) {
			console.log(data.errorMessage);
		}
	}
	
	// Event handler to catch search form submit
	$scope.search = function() {
		var params = { q: $scope.query, additionalFields: $scope.additionalFields };
		
		$http({
			method: 'GET',
			url: '/service/search',
			params: params
		}).success(function(data) {
			if (data.error) {
				self.logError(data.errorMessage);
				
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
	
	$scope.hasRelated = function(result) {
		// Use underscore.js's size function
		return _.size(result.relatedLabels) > 0;
	}
	
	$scope.formatRelatedType = function(type) {
		var ret = '';
		var words = type.split('_');
		for (var i = 0; i < words.length - 2; i ++) {
			ret = ret + (i > 0 ? ' ' : '') + words[i];
		}
		ret = ret.charAt(0).toUpperCase() + ret.slice(1);
		return ret;
	}
	
	// Initialise the page
	self.init();
	
}]);