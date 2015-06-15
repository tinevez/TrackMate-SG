package fiji.plugin.trackmate.extras.action;

import ij.measure.ResultsTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TableExportAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TableExportAction.class.getResource( "Cellprofiler-icon-16x16.png" ) );

	public static final String NAME = "Export statistics to Excel tables";

	public static final String KEY = "EXPORT_STATS_TO_XLS";

	public static final String INFO_TEXT = "<html>" + "Compute and export track statistics to an Excel file."
			+ "<p>"
			+ "The table is formatted to resemble a CellProfiler table." + "</html>";
	
	private static final String[] TABLE_HEADERS_SF = new String[] {
		"AreaShape_Center_X",
		"AreaShape_Center_Y",
		"AreaShape_MeanRadius",
		"Intensity_MeanIntensity_OrigGray",
		"Intensity_MedianIntensity_OrigGray"
	};
	
	private static final String[] MATCHING_SF = new String[] {
			Spot.POSITION_X,
			Spot.POSITION_Y,
			Spot.RADIUS,
			SpotIntensityAnalyzerFactory.MEAN_INTENSITY,
			SpotIntensityAnalyzerFactory.MEDIAN_INTENSITY
	};

	private static final String FRAME_FEATURE = "ImageNumber";
	
	private static final String SPOT_ID_FEATURE = "ObjectNumber";

	private static final String TRACK_ID_FEATURE = "TrackObjects_ParentObjectNumber_50";

	private static final String FILE_FEATURE = "Metadata_FileLocation";

	@Override
	public void execute( final TrackMate trackmate )
	{
		// Model
		final Model model = trackmate.getModel();
		final FeatureModel fm = model.getFeatureModel();

		// File
		final String fileLoc = "file:" + new File( trackmate.getSettings().imageFolder, trackmate.getSettings().imageFileName ).getAbsolutePath();

		// Export tracks
		logger.log( "Generating table..." );

		// Create table
		final ResultsTable table = new ResultsTable();

		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );

		// Sort by track
		for ( final Integer trackID : trackIDs )
		{
			final Set< Spot > setSpots = model.getTrackModel().trackSpots( trackID );

			// Sort spots per frame
			final List< Spot > spots = new ArrayList< Spot >( setSpots );
			Collections.sort( spots, Spot.frameComparator );

			for ( final Spot spot : spots )
			{
				table.incrementCounter();

				/*
				 * First 3 columns: frame, spot id, track id and file location.
				 */

				// 0. Frame.
				final int frame = spot.getFeature( Spot.FRAME ).intValue();
				table.addValue( FRAME_FEATURE, "" + frame );

				// 1. Spot ID.
				final int spotID = spot.ID();
				table.addValue( SPOT_ID_FEATURE, "" + spotID );

				// 2. Track ID.
				table.addValue( TRACK_ID_FEATURE, trackID.toString() );

				// 3. File location.
				table.addValue( FILE_FEATURE, fileLoc );

				/*
				 * Other features, taken from spot features.
				 */

				for ( int i = 0; i < TABLE_HEADERS_SF.length; i++ )
				{
					final String header = TABLE_HEADERS_SF[ i ];
					final String feature = MATCHING_SF[ i ];

					final Double val = spot.getFeature( feature );
					if ( null == val )
					{
						table.addValue( header, "None" );
					}
					else
					{
						if ( fm.getSpotFeatureIsInt().get( feature ).booleanValue() )
						{
							table.addValue( header, "" + val.intValue() );
						}
						else
						{
							table.addValue( header, val.doubleValue() );
						}
					}
				}
			}
		}
		logger.log( " Done.\n" );

		// Show table
		table.show( "Track statistics" );
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new TableExportAction();
		}
	}

	/*
	 * TEST
	 */

	public static void main( final String[] args )
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.err.println( "Error reading file " + file + ":\n" + reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();

		final Settings settings = new Settings();
		reader.readSettings( settings, new DetectorProvider(), new TrackerProvider(), new SpotAnalyzerProvider(), new EdgeAnalyzerProvider(), new TrackAnalyzerProvider() );

		final TrackMate trackmate = new TrackMate( model, settings );

		new TableExportAction().execute( trackmate );

	}

}
