(function () {

  //// Websocket functions

  function onOpen(event) {
    console.log(`Websocket channel opened on ${event.target.url}`, event);
  }

  function onClose(event) {
    console.log('Websocket channel closed:', event);
  }

  function onMessage(event) {
    console.log('Received message:', JSON.parse(event.data));
  }

  function onError(event) {
    console.log('Error:', event);
  }

  const websocket = new WebSocket(`ws://${window.location.host}/ws`);
  websocket.onopen = onOpen;
  websocket.onclose = onClose;
  websocket.onmessage = onMessage;
  websocket.onerror = onError;

  //// Get the canvas element and context

  const canvas = document.getElementById('canvas');
  const canvasContext = canvas.getContext('2d');
  
  //// Initialize context

  canvasContext.fillStyle = 'rgba(0, 0, 200, 0.5)';

  //// Configuration

  const clickEvent = null;
  const mouseMoveEvent = null;
  const drawWidth = 50;
  const drawHeight = 50;
  var mouseDown = false;

  //// Utility functions

  function getMousePosition(canvas, event) {
    const rect = canvas.getBoundingClientRect();
    return {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top
    };
  }

  function drawRectangle(ctx, mousePosition, width, height) {
    console.log('drawing rectangle');
    const x = mousePosition.x - (width / 2);
    const y = mousePosition.y - (height / 2);
    ctx.fillRect(x, y, width, height);
    websocket.send(JSON.stringify({
      x,
      y,
      width,
      height,
      fillStyle: ctx.fillStyle
    }));
  }

  //// Event listeners

  canvas.addEventListener('click', function (event) {
    drawRectangle(canvasContext, getMousePosition(canvas, event), drawWidth, drawHeight);
  });

  canvas.addEventListener('mousedown', function (event) {
    mouseDown = true;
  });

  canvas.addEventListener('mouseup', function (event) {
    mouseDown = false;
  });

  canvas.addEventListener('mousemove', function(event) {
    if (mouseDown) {
      drawRectangle(canvasContext, getMousePosition(canvas, event), drawWidth, drawHeight);
    }
  });  
})();


