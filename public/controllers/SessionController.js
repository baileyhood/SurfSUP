angular
  .module('surfSup')
  .controller('SessionController', function($scope, $location, SessionService, CacheEngine) {

    $scope.addSesh = addSesh;
    $scope.deleteSession = deleteSession;
    $scope.editSession = editSession;

    // CacheEngine
    if (CacheEngine.get('seshActivity')){
      var cache = CacheEngine.get('seshActivity');
      $scope.seshActivity = cache.data;
      console.log('cache is working! seshActivity =', cache);
    }
    else {
        SessionService.getSession()
        .then(function(data) {
          CacheEngine.put('seshActivity', data);
          $scope.seshActivity = data.data;
          window.glow = data.data;
          console.log('data pulling is working! seshActivity =', data);
        });
    }

    // addSesh
    function addSesh () {
      $scope.sessionObjs = {
        time: $scope.time.toISOString().slice(0,19),
        isSurf: $scope.suppy,
        location: $scope.location
      };
      console.log("session obj", $scope.sessionObjs);
      SessionService.addSession($scope.sessionObjs).success(function(res){
        console.log('session created', res);
        $location.path('/sessions');
        $scope.$apply();
      })
      .error(function(err) {
        console.log('doh', err);
        $('#sessionTime').html('<div class="alert alert-danger" role="alert"><strong>Oh no!</strong> The username and password do not match. Try again.</div>');
      });
    }

    // addSesh update
    $scope.$on('session:added', function() {
      SessionService.getSession()
        .success(function(sessions) {
          $scope.seshActivity = sessions;
        })
    })

    // deleteSession
    function deleteSession(id) {
      console.log('this is id', id);
      SessionService.deleteSesh(id)
      .then(function(data) {
        var objId = id;
        var objPlace = $scope.seshActivity.findIndex (function(el,idx,arr){
          return el.id === objId;
        });
        $scope.seshActivity.splice (objPlace, 1);
        console.log('deny requests', objPlace);
      });

    }

    // editedSession
    function editSession(id,location) {
      console.log('location',location);
      SessionService.editSession(id,location);
    }



  }); // end of AddSessionController
