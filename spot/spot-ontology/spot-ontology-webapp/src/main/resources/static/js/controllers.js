ontologyApp
.controller('SearchCtrl', ['$scope', '$http', function($scope, $http) {
	
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
		// Kludge so we can use checklist-model for the parent/child label checkboxes
		$scope.efo_child_labels = 'efo_child_labels';
		$scope.efo_parent_labels = 'efo_parent_labels';
	}
	
	self.logError = function(errorMsg) {
		if (window.console) {
			console.log(data.errorMessage);
		}
	}
	
	$scope.changePage = function() {
		var start = 10 * ($scope.currentPage - 1);
		var params = { q: $scope.query, additionalFields: $scope.additionalFields, start: start }
		self.updateModel(params);
	}
	
	// Event handler to catch search form submit
	$scope.search = function() {
		var params = { q: $scope.query, additionalFields: $scope.additionalFields };
		self.updateModel(params);
	}
	
	$scope.addFilter = function(field, term) {
		var filter = field + ':"' + term + '"';
		if ($scope.fq) {
			$scope.fq.push(filter);
			$scope.appliedFilters.push({ field: term });
		} else {
			$scope.fq = [ filter ];
			$scope.appliedFilters = [{ field: term}];
		}
		
		var params = { q: $scope.query, additionalFields: $scope.additionalFields, fq: $scope.fq };
		self.updateModel(params);
	}
	
	self.updateModel = function(params) {
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
				$scope.currentPage = (data.start / data.rows) + 1;
				$scope.facets = data.facets;
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
	
	$scope.getFacetLabel = function(label) {
		var ret = label;
		
		if (label == 'efo_child_labels_str') {
			ret = 'Child labels';
		} else if (label == 'efo_labels_str') {
			ret = 'Labels';
		}
		
		return ret;
	}
	
	$scope.showTopLevelFacets = function() {
		return $scope.fq == undefined || $scope.fq.length == 0;
	}
	
	$scope.showSecondLevelFacets = function() {
		return $scope.filtersApplied();
	}
	
	$scope.filtersApplied = function() {
		return $scope.fq && $scope.fq.length > 0;
	}
	
	// Initialise the page
	self.init();
	
}])
.controller('SparqlCtrl', ['$scope', '$http', function($scope, $http) {
	
	var self = this;
	
	$scope.prefix = 'PREFIX : <http://example/>\nPREFIX text: <http://jena.apache.org/text#>\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>';
	$scope.query = 'SELECT *\n{ ?s text:query (rdfs:label \'lung\') ;\nrdfs:label ?label\n}';
	
	$scope.search = function() {
		var queryData = {
				'prefix': $scope.prefix,
				'query': $scope.query,
				'rows': 10
		};
		
		$http({
			method: 'POST',
			url: '/service/jenaSearch',
			data: queryData
		}).success(function(data) {
			$scope.results = data.results;
		});
	}
	
	$scope.resultKeys = function() {
		var ret = [];
		
		if ($scope.results && $scope.results.length > 0) {
			var keys = Object.keys($scope.results[0]);
			var result = $scope.results[0];
			for (i = 0; i < keys.length; i ++) {
				if (keys[i].indexOf('$') == -1) {
					ret.push(keys[i]);
				}
			}
		}
		
		return ret;
	}
	
}]);