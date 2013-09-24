package com.kurento.kmf.media.objects;

import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public abstract class UriEndPoint extends EndPoint {

	UriEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	// static <T extends zUriEndPoint> UriEndPointType getType(Class<T> type) {
	// try {
	// Field field = type.getDeclaredField(URI_END_POINT_TYPE_FIELD_NAME);
	// return (UriEndPointType) field.get(type);
	// } catch (NoSuchFieldException e) {
	// throw new IllegalArgumentException(e);
	// } catch (SecurityException e) {
	// throw new IllegalArgumentException(e);
	// } catch (IllegalArgumentException e) {
	// throw new IllegalArgumentException(e);
	// } catch (IllegalAccessException e) {
	// throw new IllegalArgumentException(e);
	// }
	// }
	//
	// /* SYNC */
	//
	// public String getUri() throws IOException {
	// MediaServerService.Client service = mssm.getMediaServerService();
	//
	// try {
	// return service.getUri(mediaObjectId);
	// } catch (MediaObjectNotFoundException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (MediaServerException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerService(service);
	// }
	// }
	//
	// protected void start() throws IOException {
	// MediaServerService.Client service = mssm.getMediaServerService();
	//
	// try {
	// service.start(mediaObjectId);
	// } catch (MediaObjectNotFoundException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (MediaServerException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerService(service);
	// }
	// }
	//
	// public void pause() throws IOException {
	// MediaServerService.Client service = mssm.getMediaServerService();
	//
	// try {
	// service.pause(mediaObjectId);
	// } catch (MediaObjectNotFoundException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (MediaServerException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerService(service);
	// }
	// }
	//
	// public void stop() throws IOException {
	// MediaServerService.Client service = mssm.getMediaServerService();
	//
	// try {
	// service.stop(mediaObjectId);
	// } catch (MediaObjectNotFoundException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (MediaServerException e) {
	// throw new RuntimeException(e.getMessage(), e);
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerService(service);
	// }
	// }
	//
	// /* ASYNC */
	//
	// public void getUri(final Continuation<String> cont) throws IOException {
	// MediaServerService.AsyncClient service = mssm
	// .getMediaServerServiceAsync();
	//
	// try {
	// service.getUri(
	// mediaObjectId,
	// new AsyncMethodCallback<MediaServerService.AsyncClient.getUri_call>() {
	// @Override
	// public void onComplete(getUri_call response) {
	// try {
	// cont.onSuccess(response.getResult());
	// } catch (MediaObjectNotFoundException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (MediaServerException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (TException e) {
	// cont.onError(new IOException(e.getMessage(), e));
	// }
	// }
	//
	// @Override
	// public void onError(Exception exception) {
	// cont.onError(exception);
	// }
	// });
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerServiceAsync(service);
	// }
	// }
	//
	// protected void start(final Continuation<Void> cont) throws IOException {
	// MediaServerService.AsyncClient service = mssm
	// .getMediaServerServiceAsync();
	//
	// try {
	// service.start(
	// mediaObjectId,
	// new AsyncMethodCallback<MediaServerService.AsyncClient.start_call>() {
	// @Override
	// public void onComplete(start_call response) {
	// try {
	// response.getResult();
	// cont.onSuccess(null);
	// } catch (MediaObjectNotFoundException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (MediaServerException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (TException e) {
	// cont.onError(new IOException(e.getMessage(), e));
	// }
	// }
	//
	// @Override
	// public void onError(Exception exception) {
	// cont.onError(exception);
	// }
	// });
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerServiceAsync(service);
	// }
	// }
	//
	// public void pause(final Continuation<Void> cont) throws IOException {
	// MediaServerService.AsyncClient service = mssm
	// .getMediaServerServiceAsync();
	//
	// try {
	// service.pause(
	// mediaObjectId,
	// new AsyncMethodCallback<MediaServerService.AsyncClient.pause_call>() {
	// @Override
	// public void onComplete(pause_call response) {
	// try {
	// response.getResult();
	// cont.onSuccess(null);
	// } catch (MediaObjectNotFoundException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (MediaServerException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (TException e) {
	// cont.onError(new IOException(e.getMessage(), e));
	// }
	// }
	//
	// @Override
	// public void onError(Exception exception) {
	// cont.onError(exception);
	// }
	// });
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerServiceAsync(service);
	// }
	// }
	//
	// public void stop(final Continuation<Void> cont) throws IOException {
	// MediaServerService.AsyncClient service = mssm
	// .getMediaServerServiceAsync();
	//
	// try {
	// service.stop(
	// mediaObjectId,
	// new AsyncMethodCallback<MediaServerService.AsyncClient.stop_call>() {
	// @Override
	// public void onComplete(stop_call response) {
	// try {
	// response.getResult();
	// cont.onSuccess(null);
	// } catch (MediaObjectNotFoundException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (MediaServerException e) {
	// cont.onError(new RuntimeException(e
	// .getMessage(), e));
	// } catch (TException e) {
	// cont.onError(new IOException(e.getMessage(), e));
	// }
	// }
	//
	// @Override
	// public void onError(Exception exception) {
	// cont.onError(exception);
	// }
	// });
	// } catch (TException e) {
	// throw new IOException(e.getMessage(), e);
	// } finally {
	// mssm.releaseMediaServerServiceAsync(service);
	// }
	// }
	//
}
