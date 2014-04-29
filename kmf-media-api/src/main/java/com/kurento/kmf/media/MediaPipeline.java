/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.FactoryMethod;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * A pipeline is a container for a collection of {@link MediaElement
 * MediaElements} and {@link MediaMixer MediaMixers}. It offers the methods
 * needed to control the creation and connection of elements inside a certain
 * pipeline.
 * 
 **/
@RemoteClass
public interface MediaPipeline extends MediaObject {

	/**
	 * Get a {@link PlayerEndpoint}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract PlayerEndpoint.Builder newPlayerEndpoint(
			@Param("uri") String uri);

	/**
	 * Get a {@link HttpGetEndpoint}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract HttpGetEndpoint.Builder newHttpGetEndpoint();

	/**
	 * Get a {@link WebRtcEndpoint}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract WebRtcEndpoint.Builder newWebRtcEndpoint();

	/**
	 * Get a {@link ZBarFilter}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract ZBarFilter.Builder newZBarFilter();

	/**
	 * Get a {@link PointerDetectorAdvFilter}.{@link Builder} for this
	 * MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract PointerDetectorAdvFilter.Builder newPointerDetectorAdvFilter(
			@Param("calibrationRegion") WindowParam calibrationRegion);

	/**
	 * Get a {@link HttpPostEndpoint}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract HttpPostEndpoint.Builder newHttpPostEndpoint();

	/**
	 * Get a {@link RtpEndpoint}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract RtpEndpoint.Builder newRtpEndpoint();

	/**
	 * Get a {@link PointerDetectorFilter}.{@link Builder} for this
	 * MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract PointerDetectorFilter.Builder newPointerDetectorFilter();

	/**
	 * Get a {@link ChromaFilter}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract ChromaFilter.Builder newChromaFilter(
			@Param("window") WindowParam window);

	/**
	 * Get a {@link Dispatcher}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract Dispatcher.Builder newDispatcher();

	/**
	 * Get a {@link DispatcherOneToMany}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract DispatcherOneToMany.Builder newDispatcherOneToMany();

	/**
	 * Get a {@link Composite}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract Composite.Builder newComposite();

	/**
	 * Get a {@link JackVaderFilter}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract JackVaderFilter.Builder newJackVaderFilter();

	/**
	 * Get a {@link FaceOverlayFilter}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract FaceOverlayFilter.Builder newFaceOverlayFilter();

	/**
	 * Get a {@link PlateDetectorFilter}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract PlateDetectorFilter.Builder newPlateDetectorFilter();

	/**
	 * Get a {@link RecorderEndpoint}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract RecorderEndpoint.Builder newRecorderEndpoint(
			@Param("uri") String uri);

	/**
	 * Get a {@link GStreamerFilter}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract GStreamerFilter.Builder newGStreamerFilter(
			@Param("command") String command);

	/**
	 * Get a {@link CrowdDetectorFilter}.{@link Builder} for this MediaPipeline
	 * 
	 **/
	@FactoryMethod("mediaPipeline")
	public abstract CrowdDetectorFilter.Builder newCrowdDetectorFilter(
			@Param("rois") List<RegionOfInterest> rois);

	/**
	 * 
	 * Factory for building {@link MediaPipeline}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for MediaPipeline
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create();
	}

	public interface Builder extends AbstractBuilder<MediaPipeline> {

	}
}
