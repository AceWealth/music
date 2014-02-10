package com.sismics.music.core.dao.dbi;

import com.google.common.base.Joiner;
import com.sismics.music.core.dao.dbi.criteria.TrackCriteria;
import com.sismics.music.core.dao.dbi.dto.TrackDto;
import com.sismics.music.core.dao.dbi.mapper.TrackMapper;
import com.sismics.music.core.model.dbi.Track;
import com.sismics.music.core.util.dbi.*;
import com.sismics.util.context.ThreadLocalContext;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import java.util.*;

/**
 * Track DAO.
 * 
 * @author jtremeaux
 */
public class TrackDao {
    /**
     * Creates a new track.
     * 
     * @param track Track to create
     * @return Track ID
     */
    public String create(Track track) {
        track.setId(UUID.randomUUID().toString());
        track.setCreateDate(new Date());

        final Handle handle = ThreadLocalContext.get().getHandle();
        handle.createStatement("insert into " +
                "  T_TRACK(TRK_ID_C, TRK_IDALBUM_C, TRK_IDARTIST_C, TRK_FILENAME_C, TRK_TITLE_C, TRK_YEAR_N, TRK_LENGTH_N, TRK_BITRATE_N, TRK_VBR_B, TRK_FORMAT_C, TRK_CREATEDATE_D)" +
                "  values(:id, :albumId, :artistId, :fileName, :title, :year, :length, :bitrate, :vbr, :format, :createDate)")
                .bind("id", track.getId())
                .bind("albumId", track.getAlbumId())
                .bind("artistId", track.getArtistId())
                .bind("fileName", track.getFileName())
                .bind("title", track.getTitle())
                .bind("year", track.getYear())
                .bind("length", track.getLength())
                .bind("bitrate", track.getBitrate())
                .bind("vbr", track.isVbr())
                .bind("format", track.getFormat())
                .bind("createDate", track.getCreateDate())
                .execute();

        return track.getId();
    }
    
    /**
     * Updates a track.
     * 
     * @param track Track to update
     * @return Updated track
     */
    public Track update(Track track) {
//        EntityManager em = ThreadLocalContext.get().getEntityManager();
//
//        // Get the track
//        Query q = em.createQuery("select d from Track d where d.id = :id and d.deleteDate is null");
//        q.setParameter("id", track.getId());
//        Track trackFromDb = (Track) q.getSingleResult();

        // Update the track

        return track;
    }
    
    /**
     * Gets an active track by its file name.
     * 
     * @param directoryId Directory ID
     * @param fileName Track file name
     * @return Track
     */
    public Track getActiveByDirectoryAndFilename(String directoryId, String fileName) {
        final Handle handle = ThreadLocalContext.get().getHandle();
        return handle.createQuery("select " + new TrackMapper().getJoinedColumns("t") +
                "  from T_TRACK t, T_ALBUM a" +
                "  where t.TRK_FILENAME_C = :fileName and t.TRK_DELETEDATE_D is null " +
                "  and a.ALB_ID_C = t.TRK_IDALBUM_C and a.ALB_IDDIRECTORY_C = :directoryId and a.ALB_DELETEDATE_D is null")
                .bind("directoryId", directoryId)
                .bind("fileName", fileName)
                .mapTo(Track.class)
                .first();
    }
    
    /**
     * Gets an active track by its trackname.
     *
     * @param id Track ID
     * @return Track
     */
    public Track getActiveById(String id) {
        final Handle handle = ThreadLocalContext.get().getHandle();
        return handle.createQuery("select " + new TrackMapper().getJoinedColumns("t") +
                "  from T_TRACK t " +
                "  where t.TRK_ID_C = :id ")
                .bind("id", id)
                .mapTo(Track.class)
                .first();
    }

    /**
     * Searches tracks by criteria.
     *
     * @param criteria Search criteria
     * @param paginatedList Paginated list (populated by side effects)
     */
    public void findByCriteria(TrackCriteria criteria, PaginatedList<TrackDto> paginatedList) {
        QueryParam queryParam = getQueryParam(criteria);
        List<Object[]> l = PaginatedLists.executePaginatedQuery(paginatedList, queryParam, false);
        List<TrackDto> trackDtoList = assembleResultList(l);
        paginatedList.setResultList(trackDtoList);
    }

    /**
     * Searches tracks by criteria.
     *
     * @param criteria Search criteria
     * @return List of tracks
     */
    public List<TrackDto> findByCriteria(TrackCriteria criteria) {
        QueryParam queryParam = getQueryParam(criteria);
        Query q = QueryUtil.getNativeQuery(queryParam);
        List<Object[]> l = q.map(ColumnIndexMapper.INSTANCE).list();
        return assembleResultList(l);
    }

    /**
     * Creates the query parameters from the criteria.
     *
     * @param criteria Search criteria
     * @return Query parameters
     */
    private QueryParam getQueryParam(TrackCriteria criteria) {
        Map<String, Object> parameterMap = new HashMap<String, Object>();

        StringBuilder sb = new StringBuilder("select t.TRK_ID_C, t.TRK_FILENAME_C, t.TRK_TITLE_C, t.TRK_YEAR_N, t.TRK_LENGTH_N, t.TRK_BITRATE_N, t.TRK_VBR_B, t.TRK_FORMAT_C, ");
        sb.append(" a.ART_ID_C, a.ART_NAME_C, t.TRK_IDALBUM_C, alb.ALB_NAME_C ");
        sb.append(" from T_TRACK t ");
        sb.append(" join T_ARTIST a ON(a.ART_ID_C = t.TRK_IDARTIST_C) ");
        sb.append(" join T_ALBUM alb ON(t.TRK_IDALBUM_C = alb.ALB_ID_C) ");
        if (criteria.getUserId() != null) {
            sb.append(" join T_PLAYLIST_TRACK pt ON(pt.PLT_IDTRACK_C = t.TRK_ID_C) ");
            sb.append(" join T_PLAYLIST p ON(p.PLL_ID_C = pt.PLT_IDPLAYLIST_C) ");
        }

        // Adds search criteria
        List<String> criteriaList = new ArrayList<String>();
        if (criteria.getAlbumId() != null) {
            criteriaList.add("t.TRK_IDALBUM_C = :albumId");
            parameterMap.put("albumId", criteria.getAlbumId());
        }
        if (criteria.getTitleLike() != null) {
            criteriaList.add("lower(t.TRK_TITLE_C) like lower(:titleLike)");
            parameterMap.put("titleLike", "%" + criteria.getTitleLike() + "%");
        }
        if (criteria.getUserId() != null) {
            criteriaList.add("p.PLL_IDUSER_C = :userId");
            parameterMap.put("userId", criteria.getUserId());
        }
        criteriaList.add("t.TRK_DELETEDATE_D is null");

        if (!criteriaList.isEmpty()) {
            sb.append(" where ");
            sb.append(Joiner.on(" and ").join(criteriaList));
        }

        if (criteria.getUserId() != null) {
            sb.append(" order by pt.PLT_ORDER_N asc");
        } else {
            sb.append(" order by t.TRK_TITLE_C asc"); //TODO add order column
        }

        QueryParam queryParam = new QueryParam(sb.toString(), parameterMap);
        return queryParam;
    }

    /**
     * Assemble the query results.
     *
     * @param l Query results as a table
     * @return Query results as a list of domain objects
     */
    private List<TrackDto> assembleResultList(List<Object[]> l) {
        List<TrackDto> trackDtoList = new ArrayList<TrackDto>();
        for (Object[] o : l) {
            int i = 0;
            TrackDto trackDto = new TrackDto();
            trackDto.setId((String) o[i++]);
            trackDto.setFileName((String) o[i++]);
            trackDto.setTitle((String) o[i++]);
            trackDto.setYear((Integer) o[i++]);
            trackDto.setLength((Integer) o[i++]);
            trackDto.setBitrate((Integer) o[i++]);
            trackDto.setVbr((Boolean) o[i++]);
            trackDto.setFormat((String) o[i++]);
            trackDto.setArtistId((String) o[i++]);
            trackDto.setArtistName((String) o[i++]);
            trackDto.setAlbumId((String) o[i++]);
            trackDto.setAlbumName((String) o[i++]);
            trackDtoList.add(trackDto);
        }
        return trackDtoList;
    }

    /**
     * Deletes all tracks from an album.
     * 
     * @param albumId Album ID
     */
    public void deleteFromAlbum(String albumId) {
        final Handle handle = ThreadLocalContext.get().getHandle();
        handle.createStatement("update T_TRACK t" +
                "  set t.TRK_DELETEDATE_D = :deleteDate" +
                "  where WHERE t.TRK_DELETEDATE_D is null and t.TRK_IDALBUM_C = :albumId ")
                .bind("albumId", albumId)
                .bind("deleteDate", new Date())
                .execute();
    }

    /**
     * Deletes a track.
     *
     * @param id Track ID
     */
    public void delete(String id) {
        final Handle handle = ThreadLocalContext.get().getHandle();
        handle.createStatement("update T_TRACK t" +
                "  set t.TRK_DELETEDATE_D = :deleteDate" +
                "  where WHERE t.TRK_DELETEDATE_D is null and t.TRK_ID_C = :id ")
                .bind("id", id)
                .bind("deleteDate", new Date())
                .execute();
    }
}