package com.meta.analyzer.riot.api.aggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meta.analyzer.ApplicationProperties;
import com.meta.analyzer.jest.PutMatchData;
import com.meta.analyzer.riot.api.grabber.GetMatchHistory;
import com.meta.analyzer.riot.dto.MatchDataDto;
import com.meta.analyzer.service.RateManager;

import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;

// Given summoner Name
// Get Item %'s for all champions
/*
 * Get an entire summoners match history worth of items
 */
@Component
public class MatchesAggregator {


	@Autowired
	PutMatchData putMatchData;
	
	@Autowired
	ApplicationProperties applicationProperties;
    
    static Logger logger = Logger.getLogger(MatchesAggregator.class.getName());
    
    @Autowired
    RateManager rateManager;
    
    @Autowired
	GetMatchHistory matchHistory;
	
    
		
	public void pullAndStoreSummonerData(String summonerName) {
		/*
		 * Pull Match History
		 */
		Summoner summoner = rateManager.getSummonerByName(summonerName);
		Map<Long, Collection<Long>> championMatchMap = matchHistory.getMatchHistory(summoner);

		if (championMatchMap == null || championMatchMap.isEmpty()) {
			logger.info("Summoneor Invalid or No Champions found in match History!");
			return;
		}

		/*
		 * Pull Items.
		 * Store Match.
		 */
		if (applicationProperties.getGetOnlyMostPlayedChampion() == true) {
			storeMostPlayedChampion(championMatchMap, summoner);
		}
		else {
			storeEntireMatchHistory(championMatchMap, summoner);
		}

	}
	
	public void storeEntireMatchHistory(Map<Long, Collection<Long>> championMatchMap, Summoner summoner) {
		int currentMatch = 0;
		int totalMatches = matchHistory.getTotalMatches();
		
		for (long championID : championMatchMap.keySet()) {
			ArrayList<Long> matchIdList = (ArrayList<Long>) championMatchMap.get(championID);
			for (long matchID : matchIdList) {
				currentMatch++;
				logger.info("Match " + currentMatch + "/" + totalMatches);
				Match match = rateManager.getMatch(matchID, summoner.getAccountId());
				putMatchData.put(new MatchDataDto(match, summoner));
			}

		}
	}
	
	public long getMostPlayedChampionId(Map<Long, Collection<Long>> championMatchMap) {
		// Find Most Played Champ
		long mostPlayedChampionId = -1;
		int largestMatchList = 0;
		for (long championId : championMatchMap.keySet()) {
			if (largestMatchList < championMatchMap.get(championId).size()) {
				largestMatchList = championMatchMap.get(championId).size();
				mostPlayedChampionId = championId;
			}
		}
		return mostPlayedChampionId;
	}
	

	public void storeMostPlayedChampion(Map<Long, Collection<Long>> championMatchMap, Summoner summoner) {
		// Find Most Played Champ
		long mostPlayedChampionID = getMostPlayedChampionId(championMatchMap);
		
		ArrayList<Long> matchIdList = (ArrayList<Long>) championMatchMap.get(mostPlayedChampionID);
		int currentMatch = 0;
		int totalMatches = matchIdList.size();
			
		logger.info("Only Getting Most Played Champion");
			
		for (long matchID : matchIdList) {

			Match match = rateManager.getMatch(matchID, summoner.getAccountId());
			if (match != null) {
				logger.info("Storing Match for:" + summoner.getName() + " | " + currentMatch++ + "/" + totalMatches);
				putMatchData.put(new MatchDataDto(match, summoner));
			}
		}
	}
}

